package com.chunky.hbmnukemeka.client;

import com.chunky.hbmnukemeka.net.TorexPacket;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderObjectFactory;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import net.minecraft.client.multiplayer.ClientLevel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a simplified mushroom cloud through Distant Horizons' generic renderer, so the explosion stays
 * visible beyond the vanilla render distance where Continuum's own entity renderer is not active.
 *
 * <p>Only referenced from {@link FarTorexClient} when Distant Horizons is installed.
 * The geometry is a stack of boxes (stem, cap ring, core) that grows and darkens with the cloud's age.
 */
final class DhTorexRenderer {

    /** One registered DH box group per torex, with the quantised state it was built for. */
    private record Live(IDhApiRenderableBoxGroup group, IDhApiLevelWrapper level, int stateKey) {}

    private static final Map<Integer, Live> LIVE = new HashMap<>();

    private DhTorexRenderer() {}

    static void update(ClientLevel mcLevel, TorexPacket.Entry e, int age, boolean hide) {
        Live live = LIVE.get(e.entityId());
        if (hide) {
            if (live != null) remove(e.entityId());
            return;
        }
        IDhApiCustomRenderObjectFactory factory = DhApi.Delayed.customRenderObjectFactory;
        if (factory == null || DhApi.Delayed.worldProxy == null || !DhApi.Delayed.worldProxy.worldLoaded()) return;

        int key = age / 20; // rebuild once per second
        if (live != null && live.stateKey == key) return;

        IDhApiLevelWrapper wrapper = findWrapper(mcLevel);
        if (wrapper == null) return;
        if (live != null) remove(e.entityId());

        List<DhApiRenderableBox> boxes = buildBoxes(e.scale(), age, e.maxAge());
        if (boxes.isEmpty()) return;
        IDhApiRenderableBoxGroup group = factory.createRelativePositionedGroup(
                "hbmnukemeka:torex_" + e.entityId(), new DhApiVec3d(e.x(), e.y(), e.z()), boxes);
        wrapper.getRenderRegister().add(group);
        LIVE.put(e.entityId(), new Live(group, wrapper, key));
    }

    static void remove(int entityId) {
        Live live = LIVE.remove(entityId);
        if (live != null) live.level.getRenderRegister().remove(live.group.getId());
    }

    static void clear() {
        for (Integer id : new ArrayList<>(LIVE.keySet())) remove(id);
    }

    private static IDhApiLevelWrapper findWrapper(ClientLevel mcLevel) {
        for (IDhApiLevelWrapper w : DhApi.Delayed.worldProxy.getAllLoadedLevelWrappers()) {
            if (w.getWrappedMcObject() == mcLevel) return w;
        }
        return null;
    }

    // ----- geometry -----

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private static DhApiRenderableBox box(double x0, double y0, double z0, double x1, double y1, double z1, Color c) {
        return new DhApiRenderableBox(new DhApiVec3d(x0, y0, z0), new DhApiVec3d(x1, y1, z1), c,
                com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial.UNKNOWN);
    }

    static List<DhApiRenderableBox> buildBoxes(float scale, int age, int maxAge) {
        List<DhApiRenderableBox> out = new ArrayList<>();
        if (maxAge <= 0) maxAge = 900;
        double life = (double) age / maxAge;          // 0..1
        double unit = 70.0 * Math.max(0.5, scale);    // base size of the cloud in blocks

        // fireball in the first few seconds
        double fire = 1.0 - smooth(age / 100.0);
        if (fire > 0.02) {
            double r = unit * (0.5 + 0.9 * smooth(age / 40.0));
            out.add(box(-r, 0, -r, r, r * 1.6, r, new Color(255, 170, 40, (int) (230 * fire))));
        }

        double rise = smooth(age / (0.30 * maxAge));
        if (rise <= 0.01) return out;
        double fade = 1.0 - smooth((life - 0.80) / 0.20);          // dissolve in the last 20 percent
        if (fade <= 0.01) return out;

        double h = unit * 3.5 * rise;                // top of the stem
        double grey = smooth(life / 0.25);           // glowing orange to dirty grey
        int cr = (int) (255 - 150 * grey), cg = (int) (140 - 60 * grey), cb = (int) (50 + 30 * grey);
        Color stem = new Color(cr, cg, cb, (int) (200 * fade));
        Color cap = new Color(Math.max(60, cr - 30), Math.max(50, cg - 20), Math.max(50, cb), (int) (220 * fade));

        // stem: three tapering segments, wider at the top
        double w0 = unit * 0.10, w1 = unit * 0.16, w2 = unit * 0.22;
        out.add(box(-w0, 0, -w0, w0, h * 0.35, w0, stem));
        out.add(box(-w1, h * 0.35, -w1, w1, h * 0.70, w1, stem));
        out.add(box(-w2, h * 0.70, -w2, w2, h * 0.95, w2, stem));

        // cap: a ring of eight boxes around the top plus a core and a flat skirt
        double ringR = unit * (0.30 + 0.55 * rise), t = unit * 0.28, capY = h * 0.92;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            double cx = Math.cos(a) * ringR, cz = Math.sin(a) * ringR;
            out.add(box(cx - t, capY, cz - t, cx + t, capY + t * 1.4, cz + t, cap));
        }
        double core = ringR * 0.75;
        out.add(box(-core, capY + t * 0.4, -core, core, capY + t * 2.0, core, cap));
        double skirt = ringR + t;
        out.add(box(-skirt, capY - t * 0.5, -skirt, skirt, capY, skirt,
                new Color(cap.getRed(), cap.getGreen(), cap.getBlue(), (int) (120 * fade))));
        return out;
    }
}
