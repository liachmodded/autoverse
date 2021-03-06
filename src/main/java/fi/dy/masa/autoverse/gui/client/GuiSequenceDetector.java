package fi.dy.masa.autoverse.gui.client;

import net.minecraft.client.resources.I18n;
import fi.dy.masa.autoverse.gui.client.base.GuiAutoverseTile;
import fi.dy.masa.autoverse.gui.client.button.GuiButtonHoverText;
import fi.dy.masa.autoverse.inventory.container.ContainerSequenceDetector;
import fi.dy.masa.autoverse.tileentity.TileEntitySequenceDetector;

public class GuiSequenceDetector extends GuiAutoverseTile
{
    private final ContainerSequenceDetector containerSD;
    private final TileEntitySequenceDetector tesd;

    public GuiSequenceDetector(ContainerSequenceDetector container, TileEntitySequenceDetector te)
    {
        super(container, 176, 256, "gui.container.sequence_detector", te);

        this.containerSD = container;
        this.tesd = te;
        this.infoArea = new InfoArea(7, 147, 11, 11, "autoverse.gui.infoarea.sequence_detector");
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        String unloc = "autoverse.container.sequence_detector";
        String s = this.tesd.hasCustomName() ? this.tesd.getName() : I18n.format(unloc);
        this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 5, 0x404040);

        s = I18n.format("autoverse.gui.label.rst");
        this.fontRenderer.drawString(s, 96 - this.fontRenderer.getStringWidth(s), 17, 0x404040);

        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.end"),                            45,  25, 0x404040);
        this.fontRenderer.drawString(I18n.format("autoverse.gui.label.sequencer_programmable.sequence"), 8,  45, 0x404040);

        this.fontRenderer.drawString(String.valueOf(this.containerSD.getPulseLength()),                 37, 148, 0x404040);

        s = I18n.format("autoverse.gui.label.output_buffer");
        this.fontRenderer.drawString(s, this.xSize - 28 - this.fontRenderer.getStringWidth(s), 158, 0x404040);

        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 163, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        int x = this.guiLeft;
        int y = this.guiTop;

        this.bindTexture(this.guiTextureWidgets);

        // Draw the slot backgrounds according to how many slots the machine has at the moment
        this.drawSlotBackgrounds( 97,  15, 0, 238, this.container.getSequenceLength(0), this.container.getSequenceLength(0) * 2); // Reset
        this.drawSlotBackgrounds(  7,  55, 0, 238, 9, this.container.getSequenceLength(1)); // Detection sequence
        this.drawSlotBackgrounds(151, 150, 0, 220, 1, 1); // Output slot

        // Draw the hilighted slot backgrounds according to how many slots the detector has matched thus far
        final int matched = this.containerSD.getMatchedLength();

        x = this.guiLeft + 7;
        y = this.guiTop + 55;

        for (int slot = 0; slot < matched; ++slot)
        {
            this.drawTexturedModalRect(x, y, 238, 36, 18, 18);

            x += 18;

            if (slot % 9 == 8)
            {
                y += 18;
                x = this.guiLeft + 7;
            }
        }
    }

    @Override
    protected void createButtons()
    {
        this.addButton(new GuiButtonHoverText(0, this.guiLeft + 26, this.guiTop + 148, 8, 8, 0, 0,
                this.guiTextureWidgets, 8, 0, "autoverse.gui.label.pulse_length"));

        this.setButtonMultipliers(4, 16);
    }
}
