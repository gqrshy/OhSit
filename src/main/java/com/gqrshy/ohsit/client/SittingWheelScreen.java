package com.gqrshy.ohsit.client;

import com.gqrshy.ohsit.sit.SitPose;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

public class SittingWheelScreen extends Screen {
    private static final int SECTOR_COUNT = SitPose.values().length;
    private static final float SECTOR_ANGLE = (float) (2 * Math.PI / SECTOR_COUNT);
    private static final float START_ANGLE = (float) (-Math.PI / 2); // top center

    private static final int BG_COLOR = 0x0000008C;          // rgba(0,0,0,140)
    private static final int HOVER_COLOR = 0xFFFFFF50;       // rgba(255,255,255,80)
    private static final int BORDER_COLOR = 0xFFFFFFFF;      // white

    private float centerX;
    private float centerY;
    private float outerRadius;
    private float innerRadius;

    private int hoveredSector = -1;

    public SittingWheelScreen() {
        super(Text.translatable("screen.ohsit.sitting_menu"));
    }

    @Override
    protected void init() {
        super.init();
        centerX = this.width / 2f;
        centerY = this.height / 2f;
        outerRadius = this.height * 0.3f;
        innerRadius = outerRadius * 0.25f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        updateHoveredSector(mouseX, mouseY);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i = 0; i < SECTOR_COUNT; i++) {
            float angle1 = START_ANGLE + i * SECTOR_ANGLE;
            float angle2 = angle1 + SECTOR_ANGLE;
            boolean hovered = (i == hoveredSector);
            int color = hovered ? HOVER_COLOR : BG_COLOR;
            drawSector(context, matrix, angle1, angle2, color);
            drawSectorBorder(context, matrix, angle1, angle2);
        }

        // Draw inner circle border
        drawCircleBorder(context, matrix, innerRadius);
        // Draw outer circle border
        drawCircleBorder(context, matrix, outerRadius);

        RenderSystem.disableBlend();

        // Draw pose labels
        for (int i = 0; i < SECTOR_COUNT; i++) {
            float midAngle = START_ANGLE + (i + 0.5f) * SECTOR_ANGLE;
            float labelRadius = (innerRadius + outerRadius) / 2f;
            float labelX = centerX + (float) Math.cos(midAngle) * labelRadius;
            float labelY = centerY + (float) Math.sin(midAngle) * labelRadius;

            SitPose pose = SitPose.values()[i];
            Text label = Text.translatable("screen.ohsit.pose." + pose.name().toLowerCase());
            int textWidth = this.textRenderer.getWidth(label);

            context.drawText(this.textRenderer, label,
                    (int) (labelX - textWidth / 2f),
                    (int) (labelY - this.textRenderer.fontHeight / 2f),
                    i == hoveredSector ? 0xFFFF00 : 0xFFFFFF, true);
        }
    }

    private void updateHoveredSector(int mouseX, int mouseY) {
        float dx = mouseX - centerX;
        float dy = mouseY - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < innerRadius || dist > outerRadius) {
            hoveredSector = -1;
            return;
        }

        float angle = (float) Math.atan2(dy, dx);
        float normalized = angle - START_ANGLE;
        // Wrap to [0, 2PI)
        while (normalized < 0) normalized += 2 * Math.PI;
        while (normalized >= 2 * Math.PI) normalized -= 2 * Math.PI;

        hoveredSector = (int) (normalized / SECTOR_ANGLE);
        if (hoveredSector >= SECTOR_COUNT) hoveredSector = SECTOR_COUNT - 1;
    }

    private void drawSector(DrawContext context, Matrix4f matrix, float angle1, float angle2, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        int segments = 16;
        float step = (angle2 - angle1) / segments;

        // Build triangle fan manually: center at (0,0) offset, then inner/outer arcs
        // Each segment is a quad split into 2 triangles:
        // outer[i], outer[i+1], inner[i+1] and outer[i], inner[i+1], inner[i]
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int s = 0; s < segments; s++) {
            float a1 = angle1 + s * step;
            float a2 = angle1 + (s + 1) * step;

            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2);
            float sin2 = (float) Math.sin(a2);

            float ox1 = centerX + cos1 * outerRadius;
            float oy1 = centerY + sin1 * outerRadius;
            float ox2 = centerX + cos2 * outerRadius;
            float oy2 = centerY + sin2 * outerRadius;
            float ix1 = centerX + cos1 * innerRadius;
            float iy1 = centerY + sin1 * innerRadius;
            float ix2 = centerX + cos2 * innerRadius;
            float iy2 = centerY + sin2 * innerRadius;

            // Triangle 1: outer1, outer2, inner2
            buffer.vertex(matrix, ox1, oy1, 0).color(r, g, b, a);
            buffer.vertex(matrix, ox2, oy2, 0).color(r, g, b, a);
            buffer.vertex(matrix, ix2, iy2, 0).color(r, g, b, a);
            // Triangle 2: outer1, inner2, inner1
            buffer.vertex(matrix, ox1, oy1, 0).color(r, g, b, a);
            buffer.vertex(matrix, ix2, iy2, 0).color(r, g, b, a);
            buffer.vertex(matrix, ix1, iy1, 0).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawSectorBorder(DrawContext context, Matrix4f matrix, float angle1, float angle2) {
        float a = 1f; // fully opaque white
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float cos1 = (float) Math.cos(angle1);
        float sin1 = (float) Math.sin(angle1);

        buffer.vertex(matrix, centerX + cos1 * innerRadius, centerY + sin1 * innerRadius, 0).color(1f, 1f, 1f, a);
        buffer.vertex(matrix, centerX + cos1 * outerRadius, centerY + sin1 * outerRadius, 0).color(1f, 1f, 1f, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawCircleBorder(DrawContext context, Matrix4f matrix, float radius) {
        int segments = 64;
        float step = (float) (2 * Math.PI / segments);
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            float a1 = i * step;
            float a2 = (i + 1) * step;
            buffer.vertex(matrix, centerX + (float) Math.cos(a1) * radius, centerY + (float) Math.sin(a1) * radius, 0).color(1f, 1f, 1f, 1f);
            buffer.vertex(matrix, centerX + (float) Math.cos(a2) * radius, centerY + (float) Math.sin(a2) * radius, 0).color(1f, 1f, 1f, 1f);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredSector >= 0) {
            selectPose(SitPose.values()[hoveredSector]);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void selectPose(SitPose pose) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player instanceof net.minecraft.client.network.AbstractClientPlayerEntity player) {
            SitAnimationHelper.playAnimation(player, pose);
        }
        this.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
