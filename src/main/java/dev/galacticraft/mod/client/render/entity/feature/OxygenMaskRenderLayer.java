/*
 * Copyright (c) 2019-2025 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.galacticraft.mod.client.render.entity.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.mixin.client.AnimalModelAgeableListModel;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import org.jetbrains.annotations.Nullable;

public class OxygenMaskRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation TEXTURE = Constant.id("textures/entity/gear/oxygen_gear.png");
    private final @Nullable ModelPart head;
    private final @Nullable ModelPart mask;
    private final @Nullable ModelPart body;
    private final @Nullable ModelPart pipe;

    public OxygenMaskRenderLayer(RenderLayerParent<T, M> context) {
        super(context);
        ModelPart root;
        float y = context.getModel() instanceof EndermanModel ? -2.0F : -3.0F;
        if (context.getModel() instanceof HierarchicalModel<?> model) {
            root = model.root();
            this.head = root.getChild(PartNames.HEAD);
            this.body = root.getChild(PartNames.BODY);
        } else if (context.getModel() instanceof HumanoidModel<?> model) {
            this.head = model.head;
            this.body = model.body;
        } else if (context.getModel() instanceof AnimalModelAgeableListModel model) {
            this.head = model.callGetHeadParts().iterator().next();
            this.body = model.callGetBodyParts().iterator().next();
        } else {
            this.head = null;
            this.mask = null;
            this.body = null;
            this.pipe = null;
            return;
        }

        MeshDefinition modelData = new MeshDefinition();
        PartDefinition modelPartData = modelData.getRoot();
        if (this.head != null) {
            modelPartData.addOrReplaceChild(Constant.ModelPartName.OXYGEN_MASK, CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -9.0F, -5.0F, 10, 10, 10, new CubeDeformation(-0.1F)), PartPose.ZERO);
        }
        if (this.body != null) {
            modelPartData.addOrReplaceChild(Constant.ModelPartName.OXYGEN_PIPE, CubeListBuilder.create().texOffs(40, 6).addBox(-2.0F, y, 1.0F, 4, 6, 8, CubeDeformation.NONE), PartPose.ZERO);
        }

        root = modelPartData.bake(64, 32);
        this.mask = this.head != null ? root.getChild(Constant.ModelPartName.OXYGEN_MASK) : null;
        this.pipe = this.body != null ? root.getChild(Constant.ModelPartName.OXYGEN_PIPE) : null;
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(this.getTextureLocation(entity), true));

        matrices.pushPose();
        if (entity instanceof Zombie zombie && zombie.isBaby()) {
            matrices.scale(0.75F, 0.75F, 0.75F);
            matrices.translate(0.0F, 1.0F, 0.0F);
        }

        if (this.mask != null && entity.galacticraft$hasMask()) {
            this.mask.copyFrom(this.head);
            this.mask.render(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
        }
        if (this.pipe != null && entity.galacticraft$hasGear()) {
            this.pipe.copyFrom(this.body);
            this.pipe.render(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
        }
        matrices.popPose();
    }

    @Override
    protected ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
