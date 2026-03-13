// soundManager.js - 사운드 볼륨 관리 및 상태 유지 유틸리티

const DEFAULT_SETTINGS = {
    master: 0.8,
    bgm: 0.6,
    sfx: 0.7
};

class SoundManager {
    constructor() {
        this.settings = this.loadSettings();
    }

    loadSettings() {
        try {
            const saved = localStorage.getItem("rogue_reload_sound_settings");
            if (saved) {
                return { ...DEFAULT_SETTINGS, ...JSON.parse(saved) };
            }
        } catch (e) {
            console.error("사운드 설정을 불러오는 중 오류 발생:", e);
        }
        return { ...DEFAULT_SETTINGS };
    }

    saveSettings() {
        try {
            localStorage.setItem("rogue_reload_sound_settings", JSON.stringify(this.settings));
            // 설정 변경을 알리는 이벤트 발생
            window.dispatchEvent(new CustomEvent("sound-settings-updated", { detail: this.settings }));
        } catch (e) {
            console.error("사운드 설정을 저장하는 중 오류 발생:", e);
        }
    }

    setVolume(type, value, persist = true) {
        if (this.settings.hasOwnProperty(type)) {
            this.settings[type] = Math.max(0, Math.min(1, value));
            if (persist) {
                this.saveSettings();
            }
        }
    }

    getVolume(type) {
        const baseVolume = this.settings[type] ?? 1;
        return baseVolume * this.settings.master; // 마스터 볼륨 계수 적용
    }

    getSettings() {
        return { ...this.settings };
    }
}

const soundManager = new SoundManager();
export default soundManager;
