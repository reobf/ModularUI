package com.gtnewhorizons.modularui.common.internal.wrapper;

import static codechicken.lib.render.FontUtils.fontRenderer;

import com.gtnewhorizons.modularui.api.GlStateManager;
import com.gtnewhorizons.modularui.api.drawable.GuiHelper;
import com.gtnewhorizons.modularui.api.drawable.Text;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.math.Color;
import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;
import com.gtnewhorizons.modularui.api.screen.Cursor;
import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.widget.IVanillaSlot;
import com.gtnewhorizons.modularui.api.widget.IWidgetParent;
import com.gtnewhorizons.modularui.api.widget.Interactable;
import com.gtnewhorizons.modularui.api.widget.Widget;
import com.gtnewhorizons.modularui.config.Config;
import com.gtnewhorizons.modularui.mixins.GuiContainerMixin;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class ModularGui extends GuiContainer {

    private final ModularUIContext context;
    private Pos2d mousePos = Pos2d.ZERO;

    @Nullable
    private Interactable lastClicked;

    private long lastClick = -1;
    private long lastFocusedClick = -1;
    private int drawCalls = 0;
    private long drawTime = 0;
    private int fps = 0;

    private float partialTicks;

    public ModularGui(ModularUIContainer container) {
        super(container);
        this.context = container.getContext();
        this.context.initializeClient(this);
    }

    public ModularUIContext getContext() {
        return context;
    }

    public Cursor getCursor() {
        return context.getCursor();
    }

    public Pos2d getMousePos() {
        return mousePos;
    }

    //    @Override
    //    public void onResize(@NotNull Minecraft mc, int w, int h) {
    //        super.onResize(mc, w, h);
    //        context.resize(new Size(w, h));
    //    }

    public void setMainWindowArea(Pos2d pos, Size size) {
        this.guiLeft = pos.x;
        this.guiTop = pos.y;
        this.xSize = size.width;
        this.ySize = size.height;
    }

    @Override
    public void initGui() {
        super.initGui();
        context.resize(new Size(width, height));
        this.context.buildWindowOnStart();
        this.context.getCurrentWindow().onOpen();
    }

    public GuiContainerMixin getAccessor() {
        return (GuiContainerMixin) this;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        mousePos = new Pos2d(mouseX, mouseY);

        int i = this.guiLeft;
        int j = this.guiTop;
        this.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        // mainly for invtweaks compat
        drawVanillaElements(mouseX, mouseY, partialTicks);
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();
        getAccessor().setHoveredSlot(null);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.disableStandardItemLighting();
        this.drawGuiContainerForegroundLayer(mouseX, mouseY);
        RenderHelper.enableGUIStandardItemLighting();

        getAccessor().setHoveredSlot(null);
        Widget hovered = getCursor().getHovered();
        if (hovered instanceof IVanillaSlot) {
            getAccessor().setHoveredSlot(((IVanillaSlot) hovered).getMcSlot());
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(i, j, 0);
        //        MinecraftForge.EVENT_BUS.post(new GuiContainerEvent.DrawForeground(this, mouseX, mouseY));
        GlStateManager.popMatrix();

        InventoryPlayer inventoryplayer = this.mc.thePlayer.inventory;
        ItemStack itemstack = getAccessor().getDraggedStack() == null
                ? inventoryplayer.getItemStack()
                : getAccessor().getDraggedStack();
        GlStateManager.translate((float) i, (float) j, 0.0F);
        if (itemstack != null) {
            int k2 = getAccessor().getDraggedStack() == null ? 8 : 16;
            String s = null;

            if (getAccessor().getDraggedStack() != null && getAccessor().getIsRightMouseClick()) {
                itemstack = itemstack.copy();
                itemstack.stackSize = (int) Math.ceil((float) itemstack.stackSize / 2.0F);
            } else if (this.isDragSplitting2() && this.getDragSlots().size() > 1) {
                itemstack = itemstack.copy();
                itemstack.stackSize = getAccessor().getDragSplittingRemnant();

                if (itemstack == null) {
                    s = "" + "§e" + "0";
                }
            }

            this.drawItemStack(itemstack, mouseX - i - 8, mouseY - j - k2, s);
        }

        if (getAccessor().getReturningStack() != null) {
            float f = (float) (Minecraft.getSystemTime() - getAccessor().getReturningStackTime()) / 100.0F;

            if (f >= 1.0F) {
                f = 1.0F;
                getAccessor().setReturningStack(null);
            }

            int l2 = getAccessor().getReturningStackDestSlot().xDisplayPosition
                    - getAccessor().getTouchUpX();
            int i3 = getAccessor().getReturningStackDestSlot().yDisplayPosition
                    - getAccessor().getTouchUpY();
            int l1 = getAccessor().getTouchUpX() + (int) ((float) l2 * f);
            int i2 = getAccessor().getTouchUpY() + (int) ((float) i3 * f);
            this.drawItemStack(getAccessor().getReturningStack(), l1, i2, null);
        }

        GlStateManager.popMatrix();

        if (Config.debug) {
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            drawDebugScreen();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
    }

    private void drawItemStack(ItemStack stack, int x, int y, String altText) {
        GlStateManager.translate(0.0F, 0.0F, 32.0F);
        this.zLevel = 200.0F;
        itemRender.zLevel = 200.0F;
        FontRenderer font = stack.getItem().getFontRenderer(stack);
        if (font == null) font = fontRenderer;
        itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), stack, x, y);
        itemRender.renderItemOverlayIntoGUI(
                font, mc.getTextureManager(), stack, x, y - (getDragSlots() != null ? 0 : 8), altText);
        this.zLevel = 0.0F;
        itemRender.zLevel = 0.0F;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        if (Config.debug) {
            long time = Minecraft.getSystemTime() / 1000;
            if (drawTime != time) {
                fps = drawCalls;
                drawCalls = 0;
                drawTime = time;
            }
            drawCalls++;
        }
        context.getMainWindow().frameUpdate(partialTicks);
        if (context.getMainWindow() != context.getCurrentWindow()) {
            context.getCurrentWindow().frameUpdate(partialTicks);
        }
        drawDefaultBackground();

        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        for (ModularWindow window : context.getOpenWindowsReversed()) {
            if (window.isEnabled()) {
                window.drawWidgets(partialTicks, false);
            }
        }

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableLighting();
        RenderHelper.enableStandardItemLighting();

        this.partialTicks = partialTicks;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        GlStateManager.pushMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        Widget hovered = context.getCursor().getHovered();
        if (hovered != null && !context.getCursor().isHoldingSomething()) {
            if (hovered instanceof IVanillaSlot
                    && ((IVanillaSlot) hovered).getMcSlot().getHasStack()) {
                renderToolTip(
                        ((IVanillaSlot) hovered).getMcSlot().getStack(),
                        mouseX,
                        mouseY,
                        ((IVanillaSlot) hovered).getExtraTooltip());
            } else if (hovered.getTooltipShowUpDelay() <= context.getCursor().getTimeHovered()) {
                List<Text> tooltip = hovered.getTooltip();
                if (!tooltip.isEmpty()) {
                    GuiHelper.drawHoveringText(
                            tooltip,
                            context.getMousePos(),
                            context.getScaledScreenSize(),
                            400,
                            1,
                            false,
                            Alignment.CenterLeft);
                }
            }
        }

        if (context.getCurrentWindow().isEnabled()) {
            context.getCurrentWindow().drawWidgets(partialTicks, true);
        }
        context.getCursor().draw(partialTicks);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableLighting();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    public void drawDebugScreen() {
        Size screenSize = context.getScaledScreenSize();
        int color = Color.rgb(180, 40, 115);
        int lineY = screenSize.height - 13;
        drawString(fontRenderer, "Mouse Pos: " + getMousePos(), 5, lineY, color);
        lineY -= 11;
        drawString(fontRenderer, "FPS: " + fps, 5, screenSize.height - 24, color);
        lineY -= 11;
        Widget hovered = context.getCursor().findHoveredWidget(true);
        if (hovered != null) {
            Size size = hovered.getSize();
            Pos2d pos = hovered.getAbsolutePos();
            IWidgetParent parent = hovered.getParent();

            drawBorder(pos.x, pos.y, size.width, size.height, color, 1f);
            drawBorder(
                    parent.getAbsolutePos().x,
                    parent.getAbsolutePos().y,
                    parent.getSize().width,
                    parent.getSize().height,
                    Color.withAlpha(color, 0.3f),
                    1f);
            drawText("Pos: " + hovered.getPos(), 5, lineY, 1, color, false);
            lineY -= 11;
            drawText("Size: " + size, 5, lineY, 1, color, false);
            lineY -= 11;
            drawText(
                    "Parent: " + (parent instanceof ModularWindow ? "ModularWindow" : parent.toString()),
                    5,
                    lineY,
                    1,
                    color,
                    false);
            lineY -= 11;
            drawText("Class: " + hovered, 5, lineY, 1, color, false);
        }
        color = Color.withAlpha(color, 25);
        for (int i = 5; i < screenSize.width; i += 5) {
            drawVerticalLine(i, 0, screenSize.height, color);
        }

        for (int i = 5; i < screenSize.height; i += 5) {
            drawHorizontalLine(0, screenSize.width, i, color);
        }
        drawRect(mousePos.x, mousePos.y, mousePos.x + 1, mousePos.y + 1, Color.withAlpha(Color.GREEN.normal, 0.8f));
    }

    protected void renderToolTip(ItemStack stack, int x, int y, List<String> extraLines) {
        FontRenderer font = null;
        List lines = new ArrayList();
        if (stack != null) {
            font = stack.getItem().getFontRenderer(stack);
            lines.addAll(stack.getTooltip(context.getPlayer(), this.mc.gameSettings.advancedItemTooltips));
        }
        lines.addAll(extraLines);
        this.drawHoveringText(lines, x, y, (font == null ? fontRenderer : font));
    }

    protected void drawVanillaElements(int mouseX, int mouseY, float partialTicks) {
        for (Object guiButton : this.buttonList) {
            ((GuiButton) guiButton).drawButton(this.mc, mouseX, mouseY);
        }
        for (Object guiLabel : this.labelList) {
            ((GuiLabel) guiLabel).func_146159_a(this.mc, mouseX, mouseY);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        context.onClientTick();
        for (ModularWindow window : context.getOpenWindowsReversed()) {
            window.update();
        }
        context.getCursor().updateHovered();
        context.getCursor().onScreenUpdate();
    }

    private boolean isDoubleClick(long lastClick, long currentClick) {
        return currentClick - lastClick < 500;
    }

    //    @Override
    //    protected boolean hasClickedOutside(int p_193983_1_, int p_193983_2_, int p_193983_3_, int p_193983_4_) {
    //        for (ModularWindow window : context.getOpenWindows()) {
    //            if (Pos2d.isInside(p_193983_1_, p_193983_2_, window.getAbsolutePos(), window.getSize())) {
    //                return false;
    //            }
    //        }
    //        return super.hasClickedOutside(p_193983_1_, p_193983_2_, p_193983_3_, p_193983_4_);
    //    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        long time = Minecraft.getSystemTime();
        boolean doubleClick = isDoubleClick(lastClick, time);
        lastClick = time;
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onClick(mouseButton, doubleClick);
        }

        if (context.getCursor().onMouseClick(mouseButton)) {
            lastFocusedClick = time;
            return;
        }

        Interactable probablyClicked = null;
        boolean wasSuccess = false;
        doubleClick = isDoubleClick(lastFocusedClick, time);
        loop:
        for (Object hovered : getCursor().getAllHovered()) {
            if (context.getCursor().onHoveredClick(mouseButton, hovered)) {
                break;
            }
            if (hovered instanceof Interactable) {
                Interactable interactable = (Interactable) hovered;
                Interactable.ClickResult result =
                        interactable.onClick(mouseButton, doubleClick && lastClicked == interactable);
                switch (result) {
                    case IGNORE:
                        continue;
                    case ACKNOWLEDGED:
                        if (probablyClicked == null) {
                            probablyClicked = interactable;
                        }
                        continue;
                    case REJECT:
                        probablyClicked = null;
                        break loop;
                    case ACCEPT:
                        probablyClicked = interactable;
                        break loop;
                    case SUCCESS:
                        probablyClicked = interactable;
                        wasSuccess = true;
                        getCursor().updateFocused((Widget) interactable);
                        break loop;
                }
            }
        }
        this.lastClicked = probablyClicked;
        if (!wasSuccess) {
            getCursor().updateFocused(null);
        }
        if (probablyClicked == null) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        lastFocusedClick = time;
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseButton) {
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onClickReleased(mouseButton);
        }
        if (!context.getCursor().onMouseReleased(mouseButton)
                && (lastClicked == null || !lastClicked.onClickReleased(mouseButton))) {
            super.mouseMovedOrUp(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceLastClick);
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onMouseDragged(mouseButton, timeSinceLastClick);
        }
        if (lastClicked != null) {
            lastClicked.onMouseDragged(mouseButton, timeSinceLastClick);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        // debug mode C + CTRL + SHIFT
        if (keyCode == 46 && isCtrlKeyDown() && isShiftKeyDown()) {
            Config.debug = !Config.debug;
        }
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onKeyPressed(typedChar, keyCode);
        }

        Widget focused = getCursor().getFocused();
        if (focused instanceof Interactable && ((Interactable) focused).onKeyPressed(typedChar, keyCode)) {
            return;
        }
        for (Object hovered : getCursor().getAllHovered()) {
            if (focused != hovered
                    && hovered instanceof Interactable
                    && ((Interactable) hovered).onKeyPressed(typedChar, keyCode)) {
                return;
            }
        }

        if (keyCode == Keyboard.KEY_ESCAPE || this.mc.gameSettings.keyBindInventory.getKeyCode() == keyCode) {
            this.context.tryClose();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    public void mouseScroll(int direction) {
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onMouseScroll(direction);
        }
        Widget focused = getCursor().getFocused();
        if (focused instanceof Interactable && ((Interactable) focused).onMouseScroll(direction)) {
            return;
        }
        for (Object hovered : getCursor().getAllHovered()) {
            if (focused != hovered
                    && hovered instanceof Interactable
                    && ((Interactable) hovered).onMouseScroll(direction)) {
                return;
            }
        }
    }

    @Override
    public void onGuiClosed() {
        context.getCloseListeners().forEach(Runnable::run);
    }

    public boolean isDragSplitting2() {
        return getAccessor().isDragSplitting();
    }

    public Set<Slot> getDragSlots() {
        return getAccessor().getDragSplittingSlots();
    }

    public RenderItem getItemRenderer() {
        return itemRender;
    }

    public float getZ() {
        return zLevel;
    }

    public void setZ(float z) {
        this.zLevel = z;
    }

    public FontRenderer getFontRenderer() {
        return fontRenderer;
    }

    @SideOnly(Side.CLIENT)
    public static void drawBorder(float x, float y, float width, float height, int color, float border) {
        drawSolidRect(x - border, y - border, width + 2 * border, border, color);
        drawSolidRect(x - border, y + height, width + 2 * border, border, color);
        drawSolidRect(x - border, y, border, height, color);
        drawSolidRect(x + width, y, border, height, color);
    }

    @SideOnly(Side.CLIENT)
    public static void drawSolidRect(float x, float y, float width, float height, int color) {
        drawRect(x, y, x + width, y + height, color);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableBlend();
    }

    @SideOnly(Side.CLIENT)
    public static void drawText(String text, float x, float y, float scale, int color, boolean shadow) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        GlStateManager.disableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 0f);
        float sf = 1 / scale;
        fontRenderer.drawString(text, (int) (x * sf), (int) (y * sf), color, shadow);
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
    }

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        if (left < right) {
            float i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            float j = top;
            top = bottom;
            bottom = j;
        }

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.instance;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.color(r, g, b, a);
        tessellator.startDrawingQuads();
        //        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
        tessellator.addVertex(left, bottom, 0.0D);
        tessellator.addVertex(right, bottom, 0.0D);
        tessellator.addVertex(right, top, 0.0D);
        tessellator.addVertex(left, top, 0.0D);
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}