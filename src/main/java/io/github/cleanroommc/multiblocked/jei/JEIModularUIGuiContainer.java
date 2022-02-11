package io.github.cleanroommc.multiblocked.jei;

import io.github.cleanroommc.multiblocked.api.gui.ingredient.IIngredientSlot;
import io.github.cleanroommc.multiblocked.api.gui.modular.ModularUI;
import io.github.cleanroommc.multiblocked.api.gui.modular.ModularUIGuiContainer;
import io.github.cleanroommc.multiblocked.api.gui.widget.Widget;
import io.github.cleanroommc.multiblocked.api.gui.widget.imp.SlotWidget;
import io.github.cleanroommc.multiblocked.util.Position;
import mezz.jei.gui.recipes.RecipeLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class JEIModularUIGuiContainer extends ModularUIGuiContainer {
    private static int lastTick;
    private static Object focus;
    private RecipeLayout layout;

    public JEIModularUIGuiContainer(ModularUI modularUI) {
        super(modularUI);
        this.mc = Minecraft.getMinecraft();
        this.itemRender = mc.getRenderItem();
        this.fontRenderer = mc.fontRenderer;
    }

    public void setRecipeLayout(RecipeLayout layout) {
        modularUI.initWidgets();
        this.layout = layout;
        ScaledResolution resolution = new ScaledResolution(mc);
        this.width = resolution.getScaledWidth();
        this.height = resolution.getScaledHeight();
        modularUI.updateScreenSize(this.width, this.height);
        Position displayOffset = new Position(modularUI.getGuiLeft(), layout.getPosY());
        modularUI.guiWidgets.values().forEach(widget -> widget.setParentPosition(displayOffset));
        this.inventorySlots.inventorySlots.clear();
    }

    public void drawInfo(@Nonnull Minecraft minecraft, int mouseX, int mouseY) {
        if (minecraft.player.ticksExisted != lastTick) {
            updateScreen();
            lastTick = minecraft.player.ticksExisted;
        }
        GlStateManager.translate(-layout.getPosX(), -layout.getPosY(),0);
        drawScreen(mouseX + layout.getPosX(), mouseY + layout.getPosY(), minecraft.getRenderPartialTicks());
        GlStateManager.translate(layout.getPosX(), layout.getPosY(),0);
    }

    @Override
    public void updateScreen() {
        modularUI.guiWidgets.values().forEach(Widget::updateScreen);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        setFocus(null);
        this.hoveredSlot = null;
        drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        for (Widget widget : modularUI.getFlatVisibleWidgetCollection()) {
            if (widget instanceof SlotWidget) {
                Slot slot = ((SlotWidget) widget).getHandle();
                if (((SlotWidget.ISlotWidget) slot).isHover()) {
                    setHoveredSlot(slot);
                }
            }
        }
        drawGuiContainerForegroundLayer(partialTicks, mouseX, mouseY);
        renderHoveredToolTip(mouseX, mouseY);
        for (Widget widget : modularUI.guiWidgets.values()) {
            if (widget instanceof IIngredientSlot && widget.isVisible()) {
                Object result = ((IIngredientSlot) widget).getIngredientOverMouse(mouseX, mouseY);
                if (result != null) {
                    setFocus(result);
                    break;
                }
            }
        }
    }

    @Override
    public void superMouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void superMouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }

    @Override
    public void superMouseReleased(int mouseX, int mouseY, int state) {
    }

    public static void setFocus(Object focus) {
        JEIModularUIGuiContainer.focus = focus;
    }

    public static Object getFocus() {
        if (focus == null) return null;
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || player.ticksExisted - lastTick > 2) {
            focus = null;
        }
        return focus;
    }
}
