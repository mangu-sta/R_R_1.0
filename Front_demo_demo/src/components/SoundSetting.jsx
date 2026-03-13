import React, { useState, useEffect } from "react";
import "../css/SoundSetting.css";
import soundManager from "../game/utils/soundManager";
import { toast } from "react-toastify";

export default function SoundSetting({ onBack }) {
    const [settings, setSettings] = useState(soundManager.getSettings());

    const handleVolumeChange = (type, value) => {
        const numValue = parseFloat(value);
        setSettings(prev => ({ ...prev, [type]: numValue }));
        soundManager.setVolume(type, numValue, false); // Do not persist yet
    };

    const handleSave = () => {
        soundManager.saveSettings();
        toast.success("사운드 설정이 저장되었습니다.");
        onBack();
    };

    const formatPercent = (val) => Math.round(val * 100) + "%";

    return (
        <div className="SoundSetting-content">
            <div className="title">
                <button className="back_btn" onClick={onBack}>
                    ←
                </button>
                사운드 설정
            </div>

            <div className="setting_list">
                <div className="setting_item">
                    <div className="label_row">
                        <span>마스터 볼륨</span>
                        <span className="value">{formatPercent(settings.master)}</span>
                    </div>
                    <input
                        type="range"
                        min="0"
                        max="1"
                        step="0.01"
                        value={settings.master}
                        onChange={(e) => handleVolumeChange("master", e.target.value)}
                        className="volume_slider"
                    />
                </div>

                <div className="setting_item">
                    <div className="label_row">
                        <span>효과음 (SFX)</span>
                        <span className="value">{formatPercent(settings.sfx)}</span>
                    </div>
                    <input
                        type="range"
                        min="0"
                        max="1"
                        step="0.01"
                        value={settings.sfx}
                        onChange={(e) => handleVolumeChange("sfx", e.target.value)}
                        className="volume_slider"
                    />
                </div>
            </div>

            <div className="footer">
                <button className="save_btn" onClick={handleSave}>
                    저장하기
                </button>
                <button className="reset_btn" onClick={() => {
                    const defaults = { master: 0.8, sfx: 0.7 };
                    handleVolumeChange("master", 0.8);
                    handleVolumeChange("sfx", 0.7);
                    setSettings(prev => ({ ...prev, ...defaults }));
                }}>
                    기본값 복원
                </button>
            </div>
        </div>
    );
}
