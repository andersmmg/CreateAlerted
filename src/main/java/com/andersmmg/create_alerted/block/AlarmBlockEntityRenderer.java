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
    private static final int BEAM_COLOR_R = 255;
    private static final int BEAM_COLOR_G = 0;
    private static final int BEAM_COLOR_B = 0;
    private static final int BEAM_COLOR_A = 255;
    private static final float FADE_DURATION = 0.3f;
    private static final float SIZE_MIN_SCALE = 0.7f;
    private static RenderType additiveBeam;
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

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
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

        float alpha;
        float scale;
        if (isPowered) {
            alpha = easeInOutCubic(progress);
            scale = SIZE_MIN_SCALE + (1.0f - SIZE_MIN_SCALE) * easeInOutCubic(progress);
        } else {
            alpha = 1.0f - easeInOutCubic(progress);
            scale = 1.0f - (1.0f - SIZE_MIN_SCALE) * easeInOutCubic(progress);
        }
        int alphaInt = Math.round(alpha * BEAM_COLOR_A);

        Direction facing = blockEntity.getBlockState().getValue(AlarmBlock.FACING);
        float rotation = ((blockEntity.getLevel().getGameTime() + partialTick) * 15) % 360;

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.fromNamespaceAndPath(CreateAlerted.MODID, "block/alarm_light"));

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        applyFacingRotation(poseStack, facing);
        poseStack.translate(0, -0.2, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        VertexConsumer consumer = bufferSource.getBuffer(getAdditiveBeam());

        renderBeam(consumer, poseStack, sprite, 1.0f, alphaInt, scale);
        renderBeam(consumer, poseStack, sprite, -1.0f, alphaInt, scale);

        poseStack.popPose();
    }

    private void renderBeam(VertexConsumer consumer, PoseStack poseStack,
                            TextureAtlasSprite sprite, float direction,
                            int alpha, float scale) {
        float x0 = direction * 0.0f;
        float x1 = direction * BEAM_LENGTH;

        float nearSize = NEAR_SIZE * scale;
        float farSize = FAR_SIZE * scale;

        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();

        // Top face (+Y)
        addQuad(consumer, poseStack, alpha,
                x0, nearSize, -nearSize, x0, nearSize, nearSize,
                x1, farSize, farSize, x1, farSize, -farSize,
                u0, v0, u1, v0, u1, v1, u0, v1);

        // Bottom face (-Y)
        addQuad(consumer, poseStack, alpha,
                x0, -nearSize, nearSize, x0, -nearSize, -nearSize,
                x1, -farSize, -farSize, x1, -farSize, farSize,
                u0, v0, u1, v0, u1, v1, u0, v1);

        // Front face (+Z)
        addQuad(consumer, poseStack, alpha,
                x0, nearSize, nearSize, x0, -nearSize, nearSize,
                x1, -farSize, farSize, x1, farSize, farSize,
                u0, v0, u1, v0, u1, v1, u0, v1);

        // Back face (-Z)
        addQuad(consumer, poseStack, alpha,
                x0, -nearSize, -nearSize, x0, nearSize, -nearSize,
                x1, farSize, -farSize, x1, -farSize, -farSize,
                u0, v0, u1, v0, u1, v1, u0, v1);
    }

    private void addQuad(VertexConsumer consumer, PoseStack poseStack, int alpha,
                         float x0, float y0, float z0, float x1, float y1, float z1,
                         float x2, float y2, float z2, float x3, float y3, float z3,
                         float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
        Matrix4f matrix = poseStack.last().pose();
        consumer.addVertex(matrix, x0, y0, z0).setColor(BEAM_COLOR_R, BEAM_COLOR_G, BEAM_COLOR_B, alpha).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(BEAM_COLOR_R, BEAM_COLOR_G, BEAM_COLOR_B, alpha).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
        consumer.addVertex(matrix, x2, y2, z2).setColor(BEAM_COLOR_R, BEAM_COLOR_G, BEAM_COLOR_B, alpha).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
        consumer.addVertex(matrix, x3, y3, z3).setColor(BEAM_COLOR_R, BEAM_COLOR_G, BEAM_COLOR_B, alpha).setUv(u3, v3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0, 1, 0);
    }

    private void applyFacingRotation(PoseStack poseStack, Direction facing) {
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
}
