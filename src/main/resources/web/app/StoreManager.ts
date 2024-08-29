import Swal from "sweetalert2";
import Alpine from "alpinejs";
import { kc } from "./app";
import { idbLoad, idbSave } from "./StorageManager";
import {
  Account,
  InboundGroupSession,
  OutboundGroupSession,
  Session,
} from "@matrix-org/olm";
import hash from "./hash";

let ws: WebSocket;
type Message = {
  sender: string;
  type: string;
  body: string;
};

declare const Olm;

const textsection = document.getElementById("textsection");

const roomhtml = (roomID: string, nickname: string) => {
  return `<a
                id="${roomID}"
                hx-get="/room/${roomID}"
                hx-selector="#rightside"
                hx-target="#rightside"
                hx-push-url="true"
              >
                <div id="card"><h2>${nickname}</h2></div>
              </a>`;
};

const chathtml = (text: string, nickname: string) => {
  return `<div id="messages">
                        <h3>${nickname}</h3>
                        <span>${text}</span>
                        </div>`;
};

export const alpineinit = async () => {
  await initroom();
  Alpine.store("room", {
    otsession: new Olm.OutboundGroupSession(),
    insession: new Olm.InboundGroupSession(),
    initChat: async () => {
      const roomID = window.location.pathname.replace("/room/", "");
      if (roomID === "") {
        return;
      }
      if (ws) {
        ws.close();
      }
      ws = new WebSocket(
        "ws://localhost:8080" + window.location.pathname + "/ws",
        ["Authorization", kc.token ? kc.token : ""]
      );

      Alpine.data("message", () => ({
        textMsg: "",
        async sendMsg(msg: string) {
          const session_key = Alpine.store("room").otsession.session_key();
          const enc_msg = Alpine.store("room").otsession.encrypt(msg);
          const enc_session_key =
            Alpine.store("auth").session.encrypt(session_key);

          const encmsg = JSON.stringify({
            msg: enc_msg,
            key: enc_session_key,
          });

          ws.send(encmsg);
          this.textMsg = "";
        },
      }));

      const currRoom = Alpine.store("auth").rooms.find((t) => t._id === roomID);

      let roomSess = await idbLoad("otsession", roomID);
      if (!roomSess) {
        sessioncreate(roomID);
        roomSess = await idbLoad("otsession", roomID);
      }

      Alpine.store("room").otsession.unpickle(
        (await idbLoad("user", Alpine.store("auth").id)).password,
        roomSess
      );

      Alpine.store("auth").session.create_outbound(
        Alpine.store("auth").account,
        currRoom.participants[0].idkeys,
        currRoom.participants[0].otkeys
      );

      ws.onmessage = async (ev) => {
        const data: Message = JSON.parse(ev.data);
        switch (data.type) {
          case "ERROR":
            await Swal.fire({
              title: "You don't have enough credit\nPlease Topup",
              input: "number",
              confirmButtonText: "Top Up",
              showCancelButton: true,
              preConfirm: async (credit) => {
                fetch("/topup", {
                  method: "POST",
                  body: credit,
                  headers: {
                    Authorization: "Bearer " + kc.token,
                  },
                }).then((res) => {
                  if (res.ok) {
                    Alpine.store("auth").credit =
                      parseInt(Alpine.store("auth").credit) + parseInt(credit);
                  }
                });
              },
            });

            break;
          case "MESSAGE":
            textsection!.innerHTML += chathtml(data.body, data.sender);
            break;

          case "NOTIFICATION":
            if (data.body === "OK") {
              Alpine.store("auth").credit = Alpine.store("auth").credit - 2500;
            }

          default:
            break;
        }
      };

      // GET MESSAGES
      fetch("/api/v1" + window.location.pathname, {
        headers: {
          Authorization: "Bearer " + kc.token,
        },
      })
        .then((res) => {
          if (!res.ok) {
            return;
          }
          return res.json();
        })
        .then((json) => {
          json.forEach(async (element) => {
            const json = element;
            const msg = JSON.parse(json.text);
            let key = await idbLoad(
              "insession",
              json.roomID + ":" + json.userID.userID
            );
            if (json.userID.userID === Alpine.store("auth").id) {
              idbSave(
                "insession",
                json.roomID + ":" + json.userID.userID,
                Alpine.store("room").otsession.session_key()
              );
              Alpine.store("room").insession.create(
                Alpine.store("room").otsession.session_key()
              );
              textsection!.innerHTML += chathtml(
                Alpine.store("room").insession.decrypt(msg.msg).plaintext,
                element.userID.nickname
              );
              return;
            }
            if (!key) {
              Alpine.store("auth").session.create_inbound(
                Alpine.store("auth").account,
                msg.key.body
              );
              key = Alpine.store("auth").session.decrypt(
                msg.key.type,
                msg.key.body
              );
              idbSave("insession", json.roomID + ":" + json.userID.userID, key);
            }

            Alpine.store("room").insession.create(key);
            textsection!.innerHTML += chathtml(
              Alpine.store("room").insession.decrypt(msg.msg).plaintext,
              element.userID.nickname
            );
          });
        });
    },
    join: async () => {
      await Swal.fire({
        title: "Join Room",
        input: "text",
        confirmButtonText: "Join",
        preConfirm: async (roomID) => {
          fetch("/api/v1/room/" + roomID + "/join", {
            method: "POST",
            headers: {
              Authorization: "Bearer " + kc.token,
              "Content-Type": "application/json"
            },
            body: JSON.stringify({
              idkeys: JSON.parse(Alpine.store("auth").account.identity_keys())
                .curve25519,
              otkeys: JSON.parse(Alpine.store("auth").account.one_time_keys())
                .curve25519.AAAAAQ,
            }),
          }).then(async (res) => {
            if (res.ok) {
              await initroom();
              await sessioncreate(roomID);
              Swal.fire({
                title: "Success",
              });
            }
            return;
          });
        },
      });
    },
    register: async () => {
      await Swal.fire({
        title: "Are you sure ?",
        showCancelButton: true,
        confirmButtonColor: "#3085d6",
        cancelButtonColor: "#d33",
        confirmButtonText: "Yes",
        preConfirm: async () => {
          fetch("/api/v1/room/register", {
            method: "POST",
            headers: {
              Authorization: "Bearer " + kc.token,
              "Content-Type": "application/json",
            },
            body: JSON.stringify({
              idkeys: JSON.parse(Alpine.store("auth").account.identity_keys())
                .curve25519,
              otkeys: JSON.parse(Alpine.store("auth").account.one_time_keys())
                .curve25519.AAAAAQ,
            }),
          })
            .then((res) => {
              if (res.ok) {
                return res.json();
              }
              return;
            })
            .then(async (json) => {
              await initroom();
              await sessioncreate(json);
              Swal.fire({
                title: "Success",
              });
            });
        },
      });
    },
    leave: async () => {
      await Swal.fire({
        title: "Are you sure ?",
        showCancelButton: true,
        confirmButtonColor: "#3085d6",
        cancelButtonColor: "#d33",
        confirmButtonText: "Yes",
        preConfirm: async () => {
          fetch("/api/v1" + window.location.pathname + "/leave", {
            method: "POST",
            headers: {
              Authorization: "Bearer " + kc.token,
            },
          }).then((res) => {
            if (res.ok) {
              document.getElementById("mainchat")?.remove();
              document
                .getElementById(window.location.pathname.replace("/room/", ""))
                ?.remove();
              Swal.fire({
                title: "Success",
              });
            }
          });
        },
      });
    },
  });
};

Alpine.store("auth", {
  session: Session,
  account: Account,
  authenticated: false,
  roles: [""],
  rooms: [],
  nickname: "",
  credit: 0,
  accountManage: () => {
    kc.accountManagement();
  },
  logout: () => {
    kc.logout({ logoutMethod: "GET" });
  },
  id: "",
});

async function initroom() {
  let rooms = [""];
  const sidebar = await document.getElementById("sidebar_content");

  const result = await (
    await fetch("/api/v1/room", {
      headers: {
        Authorization: "Bearer " + kc.token,
      },
    })
  ).json();

  result.forEach((element) => {
    let participants: string[] = [];

    element.participants = element.participants.filter((t: any) => {
      const notID = t.userID.userID !== Alpine.store("auth").id;

      if (notID) {
        participants.push(t.userID.nickname);
      }

      return notID;
    });

    Alpine.store("auth").rooms.push(element);
    if (participants.length === 0) {
      rooms.push(roomhtml(element._id, "Empty Room"));
    } else {
      rooms.push(roomhtml(element._id, participants.join()));
    }
  });

  sidebar!.innerHTML = rooms.join("");
}

async function sessioncreate(roomID: string) {
  const outbound = new Olm.OutboundGroupSession();
  outbound.create();
  await idbSave(
    "otsession",
    roomID,
    outbound.pickle((await idbLoad("user", Alpine.store("auth").id)).password)
  );
}
