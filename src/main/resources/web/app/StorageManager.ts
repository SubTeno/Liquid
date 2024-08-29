let idb: IDBDatabase | null = null;

async function idbInit(): Promise<void> {
    if (!indexedDB) {
        throw new Error("IndexedDB not available");
    }
    idb = await new Promise((resolve, reject) => {
        const request = indexedDB.open("liquid", 1);
        request.onerror = reject;
        request.onsuccess = (): void => {
            resolve(request.result);
        };
        request.onupgradeneeded = (): void => {
            const db = request.result;
            db.createObjectStore("user");
            db.createObjectStore("insession");
            db.createObjectStore("otsession");
        };
    });
}

export async function idbLoad(table: string, key: string | string[]): Promise<any> {
    if (!idb) {
        await idbInit();
    }
    return new Promise((resolve, reject) => {
        const txn = idb!.transaction([table], "readonly");
        txn.onerror = reject;

        const objectStore = txn.objectStore(table);
        const request = objectStore.get(key);
        request.onerror = reject;
        request.onsuccess = (event): void => {
            resolve(request.result);
        };
    });
}


export async function idbSave(table: string, key: string | string[], data: any): Promise<void> {
    if (!idb) {
        await idbInit();
    }
    return new Promise((resolve, reject) => {
        const txn = idb!.transaction([table], "readwrite");
        txn.onerror = reject;

        const objectStore = txn.objectStore(table);
        const request = objectStore.put(data, key);
        request.onerror = reject;
        request.onsuccess = (event): void => {
            resolve();
        };
    });
}

export async function idbDelete(table: string, key: string | string[]): Promise<void> {
    if (!idb) {
        await idbInit();
    }
    return new Promise((resolve, reject) => {
        const txn = idb!.transaction([table], "readwrite");
        txn.onerror = reject;

        const objectStore = txn.objectStore(table);
        const request = objectStore.delete(key);
        request.onerror = reject;
        request.onsuccess = (): void => {
            resolve();
        };
    });
}