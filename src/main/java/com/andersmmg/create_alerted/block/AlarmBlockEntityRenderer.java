package com.andersmmg.create_alerted.block;

import com.andersmmg.create_alerted.CreateAlerted;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class AlarmBlockEntityRenderer implements BlockEntityRenderer<AlarmBlockEntity> {

    static final TransparencyStateShard ADDITIVE_TRANSPARENCY = new TransparencyStateShard(
            "additive_transparency",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(
                        GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ONE
                );
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
    );

    private static final float BEAM_LENGTH = 1.0f;
    private static final float NEAR_SIZE = 0.05f;
    private static final float FAR_SIZE = 0.2f;
    private static final int BEAM_COLOR_A = 255;
    private static final float FADE_DURATION = 0.3f;
    private static final float SIZE_MIN_SCALE = 0.7f;
    private static final float BLINK_BOX_W = 0.30f;
    private static final float BLINK_BOX_H = 0.36f;
    private static final float BLINK_BOX_D = 0.30f;
    private static final float BLINK_BOX_Y = -0.21875f;

    private static final float BLINK_FADE_TICKS = AlarmBlockEntity.BLINK_FADE_TICKS;
    private static final float BLINK_FADE_END_TICKS = AlarmBlockEntity.BLINK_FADE_END_TICKS;
    private static final float BLINK_FADE_OUT_END_TICKS = AlarmBlockEntity.BLINK_FADE_OUT_END_TICKS;
    private static final float BLINK_CYCLE_TICKS = AlarmBlockEntity.BLINK_CYCLE_TICKS;

    private static final float FLASH_ON_TICKS = AlarmBlockEntity.FLASH_ON_TICKS;
    private static final float FLASH_ON_PLATEAU_END = AlarmBlockEntity.FLASH_ON_PLATEAU_END;
    private static final float FLASH_CYCLE_TICKS = AlarmBlockEntity.FLASH_CYCLE_TICKS;
    private static final float FLASH_FADE_TICKS = AlarmBlockEntity.FLASH_FADE_TICKS;

    private static RenderType additiveBeam;
    private static final VisualRenderer DEFAULT_VISUAL = AlarmBlockEntityRenderer::renderSpin;
    private static final Map<ResourceLocation, VisualRenderer> VISUAL_RENDERERS = new HashMap<>();
    private static RenderType blinkBox;

    static {
        VISUAL_RENDERERS.put(AlarmVisualType.SPIN.id(), AlarmBlockEntityRenderer::renderSpin);
        VISUAL_RENDERERS.put(AlarmVisualType.BLINK.id(), AlarmBlockEntityRenderer::renderBlink);
        VISUAL_RENDERERS.put(AlarmVisualType.FLASHING.id(), AlarmBlockEntityRenderer::renderFlashing);
    }

    private static RenderType getBlinkBox() {
        if (blinkBox == null) {
            blinkBox = RenderType.create(
                    "alarm_blink_box",
                    DefaultVertexFormat.BLOCK,
                    VertexFormat.Mode.QUADS,
                    256, false, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(NO_OVERLAY)
                            .createCompositeState(true)
            );
        }
        return blinkBox;
    }

    private static TextureAtlasSprite getAlarmLightSprite() {
        return Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.fromNamespaceAndPath(CreateAlerted.MODID, "block/alarm_light"));
    }

    public AlarmBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static RenderType getAdditiveBeam() {
        if (additiveBeam == null) {
            additiveBeam = RenderType.create(
                    "alarm_beam_additive",
                    DefaultVertexFormat.BLOCK,
                    VertexFormat.Mode.QUADS,
                    256, false, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(NO_OVERLAY)
                            .createCompositeState(true)
            );
        }
        return additiveBeam;
    }

    private static TextureAtlasSprite getAlarmGlowSprite() {
        return Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.fromNamespaceAndPath(CreateAlerted.MODID, "block/alarm_glow"));
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
    }

    private static void setupBlockPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5, 0.5, 0.5);
        applyFacingRotation(poseStack, facing);
    }

    private static void renderSpin(VisualCtx ctx) {
        float rotation = ((ctx.gameTime + ctx.partialTick) * 15) % 360;
        int alphaInt = Math.round(ctx.powerAlpha * BEAM_COLOR_A);
        float scale = SIZE_MIN_SCALE + (1.0f - SIZE_MIN_SCALE) * ctx.powerAlpha;
        TextureAtlasSprite sprite = getAlarmLightSprite();

        ctx.pose.pushPose();
        setupBlockPose(ctx.pose, ctx.facing);
        ctx.pose.translate(0, -0.2, 0);
        ctx.pose.mulPose(Axis.YP.rotationDegrees(rotation));

        VertexConsumer consumer = ctx.buffers.getBuffer(getAdditiveBeam());
        renderBeam(consumer, ctx.pose, sprite, 1.0f, alphaInt, ctx.r, ctx.g, ctx.b, scale);
        renderBeam(consumer, ctx.pose, sprite, -1.0f, alphaInt, ctx.r, ctx.g, ctx.b, scale);

        ctx.pose.popPose();
    }

    private static void renderBlink(VisualCtx ctx) {
        // Wave: 0.2s fade-up, 1.0s on, 0.2s fade-down, 1.0s off, repeat
        float t = (ctx.gameTime + ctx.partialTick) % BLINK_CYCLE_TICKS;
        float blinkAlpha;
        if (t < BLINK_FADE_TICKS) {
            blinkAlpha = t / BLINK_FADE_TICKS;                              // fade up (0 -> 1)
        } else if (t < BLINK_FADE_END_TICKS) {
            blinkAlpha = 1.0f;                                              // on
        } else if (t < BLINK_FADE_OUT_END_TICKS) {
            blinkAlpha = (BLINK_FADE_OUT_END_TICKS - t) / BLINK_FADE_TICKS; // fade down (1 -> 0)
        } else {
            blinkAlpha = 0.0f;                                              // off
        }
        renderBoxAt(ctx, ctx.powerAlpha * blinkAlpha);
    }

    private static void renderFlashing(VisualCtx ctx) {
        // Wave: 2-tick fade-in, full on, 2-tick fade-out, 1.0s off, repeat — smooths the snap
        float t = (ctx.gameTime + ctx.partialTick) % FLASH_CYCLE_TICKS;
        float flashAlpha;
        if (t < FLASH_FADE_TICKS) {
            flashAlpha = t / FLASH_FADE_TICKS;                                  // fade in (0 -> 1)
        } else if (t < FLASH_ON_PLATEAU_END) {
            flashAlpha = 1.0f;                                                  // on
        } else if (t < FLASH_ON_TICKS) {
            flashAlpha = (FLASH_ON_TICKS - t) / FLASH_FADE_TICKS;               // fade out (1 -> 0)
        } else {
            flashAlpha = 0.0f;                                                  // off
        }
        renderBoxAt(ctx, ctx.powerAlpha * flashAlpha);
    }

    private static void renderBoxAt(VisualCtx ctx, float combinedAlpha) {
        if (combinedAlpha <= 0.01f) return;
        int alphaInt = Math.round(combinedAlpha * BEAM_COLOR_A);

        ctx.pose.pushPose();
        setupBlockPose(ctx.pose, ctx.facing);
        ctx.pose.translate(0, BLINK_BOX_Y, 0);

        VertexConsumer consumer = ctx.buffers.getBuffer(getBlinkBox());
        renderBlinkBox(consumer, ctx.pose, getAlarmGlowSprite(), alphaInt, ctx.r, ctx.g, ctx.b);

        ctx.pose.popPose();
    }

    private static void renderBeam(VertexConsumer consumer, PoseStack poseStack,
                                   TextureAtlasSprite sprite, float direction,
                                   int alpha, int r, int g, int b, float scale) {
        float x0 = direction * 0.0f;
        float x1 = direction * BEAM_LENGTH;

        float nearSize = NEAR_SIZE * scale;
        float farSize = FAR_SIZE * scale;

        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();

        addQuad(consumer, poseStack, alpha, r, g, b,
                x0, nearSize, -nearSize, x0, nearSize, nearSize,
                x1, farSize, farSize, x1, farSize, -farSize,
                u0, v0, u1, v0, u1, v1, u0, v1);

        addQuad(consumer, poseStack, alpha, r, g, b,
                x0, -nearSize, nearSize, x0, -nearSize, -nearSize,
                x1, -farSize, -farSize, x1, -farSize, farSize,
                u0, v0, u1, v0, u1, v1, u0, v1);

        addQuad(consumer, poseStack, alpha, r, g, b,
                x0, nearSize, nearSize, x0, -nearSize, nearSize,
                x1, -farSize, farSize, x1, farSize, farSize,
                u0, v0, u1, v0, u1, v1, u0, v1);

        addQuad(consumer, poseStack, alpha, r, g, b,
                x0, -nearSize, -nearSize, x0, nearSize, -nearSize,
                x1, farSize, -farSize, x1, -farSize, -farSize,
                u0, v0, u1, v0, u1, v1, u0, v1);
    }

    private static void renderBlinkBox(VertexConsumer consumer, PoseStack poseStack,
                                       TextureAtlasSprite sprite,
                                       int alpha, int r, int g, int b) {
        float hw = BLINK_BOX_W * 0.5f;
        float hh = BLINK_BOX_H * 0.5f;
        float hd = BLINK_BOX_D * 0.5f;

        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();

        addQuad(consumer, poseStack, alpha, r, g, b,
                -hw, hh, -hd, hw, hh, -hd,
                hw, hh, hd, -hw, hh, hd,
                u0, v0, u1, v0, u1, v1, u0, v1);

        addQuad(consumer, poseStack, alpha, r, g, b,
                -hw, -hh, hd, hw, -hh, hd,
                hw, -hh, -hd, -hw, -hh, -hd,
                u0, v0, u1, v0, u1, v1, u0, v1);

        addQuad(consumer, poseStack, alpha, r, g, b,
                hw, hh, -hd, hw, hh, hd,
                hw, -hh, hd, hw, -hh, -hd,
                u0, v0, u1, v0, u1, v1, u0, v1);

        addQuad(consumer, poseStack, alpha, r, g, b,
                -hw, hh, hd, -hw, hh, -hd,
                -hw, -hh, -hd, -hw, -hh, hd,
                u0, v0, u1, v0, u1, v1, u0, v1);

        addQuad(consumer, poseStack, alpha, r, g, b,
                -hw, hh, hd, hw, hh, hd,
                hw, -hh, hd, -hw, -hh, hd,
                u0, v0, u1, v0, u1, v1, u0, v1);

        addQuad(consumer, poseStack, alpha, r, g, b,
                hw, hh, -hd, -hw, hh, -hd,
                -hw, -hh, -hd, hw, -hh, -hd,
                u0, v0, u1, v0, u1, v1, u0, v1);
    }

    private static void addQuad(VertexConsumer consumer, PoseStack poseStack, int alpha, int r, int g, int b,
                                float x0, float y0, float z0, float x1, float y1, float z1,
                                float x2, float y2, float z2, float x3, float y3, float z3,
                                float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
        Matrix4f matrix = poseStack.last().pose();
        consumer.addVertex(matrix, x0, y0, z0).setColor(r, g, b, alpha).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, alpha).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, alpha).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, alpha).setUv(u3, v3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
    }

    private static void applyFacingRotation(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case UP -> {
            }
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
        }
    }

    @Override
    public void render(AlarmBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) return;

        boolean isPowered = blockEntity.getBlockState().getValue(AlarmBlock.POWERED);
        if (isPowered != blockEntity.wasPowered()) {
            blockEntity.setLastPoweredChangeTime(blockEntity.getLevel().getGameTime());
            blockEntity.setWasPowered(isPowered);
        }

        float elapsed = (blockEntity.getLevel().getGameTime() + partialTick
                - blockEntity.getLastPoweredChangeTime()) / 20.0f;
        float progress = Math.min(elapsed / FADE_DURATION, 1.0f);

        if (!isPowered && progress >= 1.0f) return;

        float powerAlpha = isPowered
                ? easeInOutCubic(progress)
                : 1.0f - easeInOutCubic(progress);

        int color = blockEntity.getColor();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        Direction facing = blockEntity.getBlockState().getValue(AlarmBlock.FACING);
        long gameTime = blockEntity.getLevel().getGameTime();

        VisualRenderer handler = VISUAL_RENDERERS.getOrDefault(
                AlarmVisualType.byId(blockEntity.getVisualId()).id(),
                DEFAULT_VISUAL
        );
        handler.render(new VisualCtx(
                poseStack, bufferSource, facing, gameTime, partialTick,
                powerAlpha, r, g, b
        ));
    }

    @FunctionalInterface
    private interface VisualRenderer {
        void render(VisualCtx ctx);
    }

    private record VisualCtx(
            PoseStack pose, MultiBufferSource buffers, Direction facing,
            long gameTime, float partialTick,
            float powerAlpha, int r, int g, int b
    ) {
    }
}
