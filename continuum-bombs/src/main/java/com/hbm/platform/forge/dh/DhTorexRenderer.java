package com.hbm.platform.forge.dh;

import com.hbm.platform.forge.dh.DhTorexPacket;
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
 * <p>Only referenced from {@link DhTorexClient} when Distant Horizons is installed.
 * The geometry is a stack of boxes (stem, cap ring, core) that grows and darkens with the cloud's age.
 */
final class DhTorexRenderer {

    /** One registered DH box group per torex, with the quantised state it was built for. */
    private record Live(IDhApiRenderableBoxGroup group, IDhApiLevelWrapper level, int stateKey) {}

    private static final Map<Integer, Live> LIVE = new HashMap<>();

    private DhTorexRenderer() {}

    static void update(ClientLevel mcLevel, DhTorexPacket.Entry e, int age, boolean hide) {
        Live live = LIVE.get(e.entityId());
        if (hide) {
            if (live != null) remove(e.entityId());
            return;
        }
        IDhApiCustomRenderObjectFactory factory = DhApi.Delayed.customRenderObjectFactory;
        if (factory == null || DhApi.Delayed.worldProxy == null || !DhApi.Delayed.worldProxy.worldLoaded()) return;

        int key = age / DhQuality.current().updateTicks; // rebuild interval by quality
        if (live != null && live.stateKey == key) return;

        IDhApiLevelWrapper wrapper = findWrapper(mcLevel);
        if (wrapper == null) return;
        if (live != null) remove(e.entityId());

        List<DhApiRenderableBox> boxes = buildBoxes(e.scale(), age, e.maxAge());
        if (boxes.isEmpty()) return;
        IDhApiRenderableBoxGroup group = factory.createRelativePositionedGroup(
                "hbm:torex_" + e.entityId(), new DhApiVec3d(e.x(), e.y(), e.z()), boxes);
        wrapper.getRenderRegister().add(group);
        if (live == null) com.hbm.Constants.LOG.info("DH torex {} drawn at {} {} {} (scale {}, age {})", e.entityId(), (int) e.x(), (int) e.y(), (int) e.z(), e.scale(), age);
        LIVE.put(e.entityId(), new Live(group, wrapper, key));
    }

    static void remove(int entityId) {
        Live live = LIVE.remove(entityId);
        if (live != null) live.level.getRenderRegister().remove(live.group.getId());
    }

    static void clear() {
        for (Integer id : new ArrayList<>(LIVE.keySet())) remove(id);
    }

    private static boolean loggedMissing;

    /**
     * The DH wrapper of the level the player is in. In singleplayer DH wraps the integrated server's level, not the
     * client level, so the identity check alone finds nothing: fall back to the same dimension.
     */
    private static IDhApiLevelWrapper findWrapper(ClientLevel mcLevel) {
        IDhApiLevelWrapper sameDimension = null;
        StringBuilder seen = new StringBuilder();
        for (IDhApiLevelWrapper w : DhApi.Delayed.worldProxy.getAllLoadedLevelWrappers()) {
            Object o = w.getWrappedMcObject();
            if (o == mcLevel) return w;
            seen.append(' ').append(o);
            if (sameDimension == null && o instanceof net.minecraft.world.level.Level l
                    && l.dimension().equals(mcLevel.dimension())) {
                sameDimension = w;
            }
        }
        if (sameDimension == null && !loggedMissing) {
            loggedMissing = true;
            com.hbm.Constants.LOG.warn("DH torex: no level wrapper for {}, DH has:{}", mcLevel.dimension(), seen);
        }
        return sameDimension;
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

    private static Color lerp(Color a, Color b, double t, int alpha) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                Math.max(0, Math.min(255, alpha)));
    }

    /** A round slab approximated by two crossed boxes (an octagon), centred on (cx, cz). */
    private static void disc(List<DhApiRenderableBox> out, double cx, double cz, double y0, double y1, double r, Color c) {
        double k = r * 0.58;
        out.add(box(cx - r, y0, cz - k, cx + r, y1, cz + k, c));
        out.add(box(cx - k, y0, cz - r, cx + k, y1, cz + r, c));
    }

    private static final Color HOT = new Color(255, 160, 45);
    private static final Color WARM = new Color(205, 95, 35);
    private static final Color SMOKE = new Color(105, 90, 82);
    private static final Color DARK = new Color(58, 50, 48);

    /**
     * A closer copy of the real cloud: a tapering round stem, a rolling torus cap (24 x 10 boxes), a hot core that
     * cools to grey with age, a condensation skirt, a ground dust ring and the opening fireball.
     */
    static List<DhApiRenderableBox> buildBoxes(float scale, int age, int maxAge) {
        DhQuality q = DhQuality.current();
        List<DhApiRenderableBox> out = new ArrayList<>(420);
        if (maxAge <= 0) maxAge = 900;
        double life = (double) age / maxAge;
        double unit = 70.0 * Math.max(0.5, scale);
        double cool = smooth(life / 0.35);                 // 0 hot .. 1 grey
        double fade = 1.0 - smooth((life - 0.82) / 0.18);  // dissolves in the last part of its life
        if (fade <= 0.01) return out;

        // opening fireball: a sphere of stacked discs
        double fire = 1.0 - smooth(age / 110.0);
        if (fire > 0.02) {
            double r = unit * (0.45 + 0.95 * smooth(age / 45.0));
            int layers = q.fireLayers;
            for (int i = 0; i < layers; i++) {
                double t = (i + 0.5) / layers * 2 - 1;     // -1 .. 1
                double lr = r * Math.sqrt(Math.max(0.05, 1 - t * t));
                double y0 = r * 0.9 + t * r * 0.9 - r * 0.9 / layers, y1 = y0 + r * 1.8 / layers;
                disc(out, 0, 0, Math.max(0, y0), Math.max(y0 + 1, y1), lr, lerp(HOT, WARM, Math.abs(t) * 0.6, (int) (235 * fire)));
            }
        }

        double rise = smooth(age / (0.30 * maxAge));
        // ground dust ring that spreads outwards
        double surge = smooth(age / (0.22 * maxAge));
        if (surge > 0.01 && surge < 1) {
            double rr = unit * (0.6 + 2.2 * surge), th = unit * 0.07;
            int n = q.dustBoxes;
            for (int i = 0; i < n; i++) {
                double a = i * 2 * Math.PI / n;
                double x = Math.cos(a) * rr, z = Math.sin(a) * rr, w = unit * 0.22;
                out.add(box(x - w, 0, z - w, x + w, th * 2.5, z + w, lerp(SMOKE, DARK, 0.3, (int) (170 * (1 - surge) * fade))));
            }
        }
        if (rise <= 0.01) return out;

        double h = unit * 3.6 * rise;                       // top of the stem
        Color stemC = lerp(WARM, SMOKE, cool, (int) (225 * fade));

        // stem: tapering round column with a slight bulge toward the top
        int rings = q.stemRings;
        for (int i = 0; i < rings; i++) {
            double t = (i + 0.5) / rings;
            double y0 = h * 0.92 * i / rings, y1 = h * 0.92 * (i + 1) / rings;
            double w = unit * (0.09 + 0.10 * t * t + 0.03 * Math.sin(t * 9 + age * 0.02));
            Color c = lerp(stemC, HOT, (1 - cool) * (1 - t) * 0.6, (int) (225 * fade));
            disc(out, 0, 0, y0, y1, w, c);
        }

        // condensation skirt about two thirds up
        double skirtY = h * 0.66;
        disc(out, 0, 0, skirtY, skirtY + unit * 0.05, unit * (0.38 + 0.35 * rise), new Color(225, 225, 230, (int) (110 * fade)));

        // rolling torus cap
        double capY = h * 0.90;
        double major = unit * (0.30 + 0.58 * rise);        // ring radius
        double minor = unit * (0.16 + 0.22 * rise);        // tube radius
        double cellH = minor * (q == DhQuality.HIGH ? 0.60 : q == DhQuality.MEDIUM ? 0.75 : 1.0);
        int around = q.torusAround, tube = q.torusTube;
        for (int i = 0; i < around; i++) {
            double a = i * 2 * Math.PI / around;
            double ca = Math.cos(a), sa = Math.sin(a);
            for (int j = 0; j < tube; j++) {
                double b = j * 2 * Math.PI / tube + age * 0.012;  // the torus rolls
                double radial = major + minor * Math.cos(b);
                double y = capY + minor * Math.sin(b) * 1.15;
                double x = ca * radial, z = sa * radial;
                double inner = 0.5 - 0.5 * Math.cos(b);             // inner tube side is hotter
                Color c = lerp(lerp(SMOKE, DARK, 0.4, 240), lerp(WARM, HOT, inner, 240), (1 - cool) * (0.4 + 0.6 * inner),
                        (int) (235 * fade));
                out.add(box(x - cellH, y - cellH, z - cellH, x + cellH, y + cellH, z + cellH, c));
            }
        }
        // hot core filling the middle of the cap and a flat top
        double core = major * 0.80;
        disc(out, 0, 0, capY - minor * 0.35, capY + minor * 0.55, core, lerp(WARM, SMOKE, cool, (int) (230 * fade)));
        disc(out, 0, 0, capY + minor * 0.55, capY + minor * 0.95, core * 0.65, lerp(SMOKE, DARK, 0.5, (int) (220 * fade)));
        return out;
    }
}
