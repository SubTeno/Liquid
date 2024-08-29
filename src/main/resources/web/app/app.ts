import keycloak from "keycloak-js";
import Swal from "sweetalert2";
import Alpine from "alpinejs";
import Olm  from "@matrix-org/olm";
import htmx from "htmx.org";
import { idbLoad, idbSave } from "./StorageManager";
import { alpineinit } from "./StoreManager";
import hash from "./hash";




declare global {
  interface Event {
    detail: any;
  }
}



const observer = new MutationObserver((mutations) => {
  mutations.forEach((mutation) => {
    mutation.addedNodes.forEach((node) => {
      if (node.nodeType === 1 && !node["htmx-internal-data"]) {
        htmx.process(node);
      }
    });
  });
});
observer.observe(document, { childList: true, subtree: true });

export const kc = new keycloak({
  url: "http://localhost:9000",
  realm: "liquid",
  clientId: "backend-service",
});

document.addEventListener("htmx:configRequest", function (evt) {
  evt.detail.headers["Authorization"] = "Bearer " + kc.token;
});

kc.onTokenExpired = () => {
  kc.login();
};


kc.onReady = async (isAuth) => {
  if (!isAuth) {
    kc.login();
    return;
  }

  const kcprofile = await kc.loadUserProfile();
  Alpine.store("auth").authenticated = isAuth;
  Alpine.store("auth").nickname = kcprofile.attributes?.nickname[0];
  Alpine.store("auth").roles = kc.realmAccess?.roles;
  Alpine.store("auth").credit = kcprofile.attributes?.credit[0];
  Alpine.store("auth").id = kcprofile.id;
  Alpine.store("auth").roles = kc.realmAccess?.roles;
  await Olm.init({ locateFile: () => "/static/olm.wasm" }).then(async () => {
    const account_pickle = await idbLoad("user", Alpine.store("auth").id);
    Alpine.store("auth").account = new Olm.Account();
    Alpine.store("auth").session = new Olm.Session();

    if (!account_pickle) {
      Alpine.store("auth").account.create();
      Alpine.store("auth").account.generate_one_time_keys(1);
      Swal.fire({
        title: "Please make a password for your private keys",
        input: "password",
        confirmButtonText: "Confirm",
        preConfirm: async (password) => {
          await idbSave("user", Alpine.store("auth").id, {
            pickle: Alpine.store("auth").account.pickle(hash(password)),
            password: hash(password),
          });
        },
      });
    } else {
      Alpine.store("auth").account.unpickle(account_pickle.password, account_pickle.pickle);
    }
  });
  
  await alpineinit();
  Alpine.start();

};

kc.init({
  onLoad: "check-sso",
  silentCheckSsoRedirectUri:
    "http://localhost:8080/static/silent-check-sso.html",
  checkLoginIframe: false,
});
