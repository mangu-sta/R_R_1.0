const DEFAULT_KEYS = {
    moveUp: "W",
    moveDown: "S",
    moveLeft: "A",
    moveRight: "D",
    fire: "LeftClick",
    aim: "RightClick",
    run: "Shift",
    reload: "R",
    chat: "Enter",
    skill: "Q",
    melee: "V",
    fireMode: "B"
};

class KeyMapping {
    constructor() {
        this.keys = { ...DEFAULT_KEYS };
        this.loadKeys();
    }

    loadKeys() {
        const savedKeys = localStorage.getItem("rogue_reload_keys");
        if (savedKeys) {
            try {
                this.keys = { ...DEFAULT_KEYS, ...JSON.parse(savedKeys) };
            } catch (e) {
                console.error("Failed to load keys from localStorage", e);
            }
        }
    }

    saveKeys(newKeys) {
        this.keys = { ...newKeys };
        localStorage.setItem("rogue_reload_keys", JSON.stringify(this.keys));
        // 알림을 통해 Phaser 시스템들이 변경사항을 인지하도록 할 수 있음
        window.dispatchEvent(new CustomEvent("keys-updated", { detail: this.keys }));
    }

    syncWithServer(jsonString) {
        if (!jsonString) {
            this.resetToDefault();
            return;
        }
        try {
            const serverKeys = JSON.parse(jsonString);
            this.keys = { ...DEFAULT_KEYS, ...serverKeys };
            localStorage.setItem("rogue_reload_keys", JSON.stringify(this.keys));
            window.dispatchEvent(new CustomEvent("keys-updated", { detail: this.keys }));
        } catch (e) {
            console.error("Failed to sync keys with server", e);
            this.resetToDefault();
        }
    }

    resetToDefault() {
        this.keys = { ...DEFAULT_KEYS };
        localStorage.removeItem("rogue_reload_keys");
        window.dispatchEvent(new CustomEvent("keys-updated", { detail: this.keys }));
    }

    getKeys() {
        return this.keys;
    }

    getPhaserKey(action) {
        const key = this.keys[action];
        if (key === "LeftClick" || key === "RightClick") return key;
        // 한글 입력 방지 및 대문자 변환 등 처리 필요할 수 있음
        return key.toUpperCase();
    }
}

const keyMapping = new KeyMapping();
export default keyMapping;
