package com.release.rr.domain.monster.ai;

import com.release.rr.domain.map.model.ObstacleRect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LineOfSightService {

    public boolean hasLineOfSight(
            float x1, float y1,
            float x2, float y2,
            List<ObstacleRect> walls
    ) {
        for (ObstacleRect w : walls) {
            if (intersects(x1, y1, x2, y2, w)) {
                return false;
            }
        }
        return true;
    }

    private boolean intersects(
            float x1, float y1,
            float x2, float y2,
            ObstacleRect r
    ) {
        float rx1 = r.x();
        float ry1 = r.y();
        float rx2 = rx1 + r.width();
        float ry2 = ry1 + r.height();

        return lineIntersectsRect(x1, y1, x2, y2, rx1, ry1, rx2, ry2);
    }

    private boolean lineIntersectsRect(
            float x1, float y1,
            float x2, float y2,
            float rx1, float ry1,
            float rx2, float ry2
    ) {
        return lineIntersectsLine(x1, y1, x2, y2, rx1, ry1, rx2, ry1) ||
                lineIntersectsLine(x1, y1, x2, y2, rx2, ry1, rx2, ry2) ||
                lineIntersectsLine(x1, y1, x2, y2, rx2, ry2, rx1, ry2) ||
                lineIntersectsLine(x1, y1, x2, y2, rx1, ry2, rx1, ry1);
    }

    private boolean lineIntersectsLine(
            float x1, float y1, float x2, float y2,
            float x3, float y3, float x4, float y4
    ) {
        float d = (x1-x2)*(y3-y4)-(y1-y2)*(x3-x4);
        if (d == 0) return false;

        float t = ((x1-x3)*(y3-y4)-(y1-y3)*(x3-x4))/d;
        float u = -((x1-x2)*(y1-y3)-(y1-y2)*(x1-x3))/d;

        return t >= 0 && t <= 1 && u >= 0 && u <= 1;
    }
}
