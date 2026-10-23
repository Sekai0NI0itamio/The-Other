package io.github.sekai0ni0itamio.theother.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.SelectWorldScreen;
import net.minecraft.network.chat.Component;

public final class TheOtherTitleScreen extends Screen {
    private static final int TOP_COLOR = 0xFF1A0303;
    private static final int BOTTOM_COLOR = 0xFF000000;
    private static final int TITLE_COLOR = 0xFFE8E8E8;
    private static final int SUBTITLE_COLOR = 0xFF8A8A8A;

    public TheOtherTitleScreen() {
        super(Component.translatable("screen.theother.title"));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 100;
        int top = this.height / 4 + 48;

        this.addRenderableWidget(Button.builder(Component.translatable("menu.singleplayer"), button -> this.minecraft.setScreen(new SelectWorldScreen(this)))
                .bounds(left, top, 200, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("menu.multiplayer"), button -> this.minecraft.setScreen(new JoinMultiplayerScreen(this)))
                .bounds(left, top + 24, 200, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("menu.options"), button -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)))
                .bounds(left, top + 48, 200, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("menu.quit"), button -> this.minecraft.stop())
                .bounds(left, top + 72, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.fillGradient(0, 0, this.width, this.height, TOP_COLOR, BOTTOM_COLOR);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4, TITLE_COLOR);
        graphics.drawCenteredString(this.font, Component.translatable("screen.theother.subtitle"), this.width / 2, this.height / 4 + 16, SUBTITLE_COLOR);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
