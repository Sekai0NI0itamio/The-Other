package io.github.sekai0ni0itamio.theother.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TheOtherTitleScreen extends Screen {
    private static final int TOP_COLOR = 0xFF170303;
    private static final int BOTTOM_COLOR = 0xFF000000;
    private static final int TITLE_COLOR = 0xFFE8E8E8;
    private static final int SUBTITLE_COLOR = 0xFF8A8A8A;
    private static final int TAGLINE_COLOR = 0xFF5C5C5C;

    public TheOtherTitleScreen() {
        super(Component.translatable("screen.theother.title"));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 100;
        int playTop = this.height / 2 - 4;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.theother.play"),
                        button -> TheOtherWorld.play(this))
                .bounds(left, playTop, 200, 24)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.theother.settings"),
                        button -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)))
                .bounds(left, playTop + 32, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.fillGradient(0, 0, this.width, this.height, TOP_COLOR, BOTTOM_COLOR);
        graphics.fill(this.width / 2 - 72, this.height / 3 + 22, this.width / 2 + 72, this.height / 3 + 23, 0xFF4A0A0A);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 3, TITLE_COLOR);
        graphics.drawCenteredString(this.font, Component.translatable("screen.theother.subtitle"), this.width / 2, this.height / 3 + 34, SUBTITLE_COLOR);
        graphics.drawCenteredString(this.font, Component.translatable("screen.theother.tagline"), this.width / 2, this.height - 28, TAGLINE_COLOR);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
