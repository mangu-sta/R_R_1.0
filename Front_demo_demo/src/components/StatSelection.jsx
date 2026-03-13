import React, { useEffect } from "react";

export default function StatSelection({ onClose, myCharacter, onSelect }) {
    
    const stats = [
        { index: 1, key: "STRENGTH", label: "힘 (1)", desc: "공격력", value: myCharacter.strength },
        { index: 2, key: "AGILITY", label: "민첩 (2)", desc: "이속", value: myCharacter.agility },
        { index: 3, key: "HEALTH", label: "체력 (3)", desc: "최대HP", value: myCharacter.health },
        { index: 4, key: "RELOAD", label: "장전 (4)", desc: "장전속도", value: myCharacter.reload },
    ];

    useEffect(() => {
        const handleKeyDown = (e) => {
            const key = e.key;
            if (key === "1") onSelect(1);
            else if (key === "2") onSelect(2);
            else if (key === "3") onSelect(3);
            else if (key === "4") onSelect(4);
        };

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [onSelect]);

    return (
        <div style={{
            position: "fixed",
            bottom: "20px",
            left: "50%",
            transform: "translateX(-50%)",
            backgroundColor: "rgba(0, 0, 0, 0.85)",
            padding: "10px 20px",
            borderRadius: "15px",
            border: "1px solid #ffcc00",
            zIndex: 3000,
            color: "white",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            boxShadow: "0 0 15px rgba(255, 204, 0, 0.3)",
            minWidth: "500px"
        }}>
            <div style={{ fontSize: "14px", fontWeight: "bold", color: "#ffcc00", marginBottom: "8px" }}>
                ✨ 레벨 {myCharacter.level} 달성! 스탯 포인트를 투자하세요 ({myCharacter.pendingStatPoints} 남아있음)
            </div>
            
            <div style={{ display: "flex", gap: "15px" }}>
                {stats.map((stat) => (
                    <div 
                        key={stat.key}
                        onClick={() => onSelect(stat.index)}
                        style={{
                            padding: "8px 12px",
                            backgroundColor: "#222",
                            border: "1px solid #444",
                            borderRadius: "8px",
                            cursor: "pointer",
                            textAlign: "center",
                            minWidth: "100px",
                            transition: "all 0.2s"
                        }}
                    >
                        <div style={{ fontSize: "16px", fontWeight: "bold" }}>{stat.label}</div>
                        <div style={{ fontSize: "11px", color: "#888" }}>{stat.desc}: {stat.value}</div>
                    </div>
                ))}
            </div>
            
            <div style={{ marginTop: "8px", fontSize: "11px", color: "#aaa" }}>
                [ 1 / 2 / 3 / 4 ] 키를 눌러 선택 가능
            </div>
        </div>
    );
}
