package zone.rong.xray.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import zone.rong.xray.FgtXRay;
import zone.rong.xray.reference.BlockInfo;
import zone.rong.xray.reference.OreInfo;

public class ClientTick implements Runnable {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final int delayMs = 200;
    private long nextTimeMs = System.currentTimeMillis();
    private Thread thread = null;

    @SubscribeEvent
    public void tickEnd(TickEvent.ClientTickEvent event) {
        if ((event.phase == TickEvent.Phase.END) && (mc.thePlayer != null)) {
            FgtXRay.localPlyX = MathHelper.floor_double(mc.thePlayer.posX);
            FgtXRay.localPlyY = MathHelper.floor_double(mc.thePlayer.posY);
            FgtXRay.localPlyZ = MathHelper.floor_double(mc.thePlayer.posZ);

            if (FgtXRay.drawOres && ((this.thread == null) || !this.thread.isAlive())
                && ((mc.theWorld != null) && (mc.thePlayer != null))) // If we're in a world and want to start drawing
                                                                      // create the thread.
            {
                this.thread = new Thread(this);
                this.thread.setDaemon(false);
                this.thread.setPriority(Thread.MAX_PRIORITY);
                this.thread.start();
            }
        }
    }

    @Override
    public void run() // Our thread code for finding ores near the player.
    {
        try {
            while (!this.thread.isInterrupted()) // Check the internal interrupt flag. Exit thread if set.
            {
                if (FgtXRay.drawOres && !OresSearch.searchList.isEmpty()
                    && (mc != null)
                    && (mc.theWorld != null)
                    && (mc.thePlayer != null)) {
                    if (nextTimeMs > System.currentTimeMillis()) // Delay to avoid spamming ore updates.
                    {
                        continue;
                    }

                    List<BlockInfo> temp = new ArrayList<>();
                    int radius = FgtXRay.distNumbers[FgtXRay.distIndex]; // Get the radius around the player to search.
                    int px = FgtXRay.localPlyX;
                    int py = FgtXRay.localPlyY;
                    int pz = FgtXRay.localPlyZ;
                    for (int y = Math.max(0, py - 96); y < py + 32; y++) // Check the y axis. from 0 or the players y-96
                                                                         // to the players y + 32
                    {
                        for (int x = px - radius; x < px + radius; x++) // Iterate the x axis around the player in
                                                                        // radius.
                        {
                            for (int z = pz - radius; z < pz + radius; z++) // z axis.
                            {
                                Block block = mc.theWorld.getBlock(x, y, z);
                                int id = Block.getIdFromBlock(block);
                                int meta = mc.theWorld.getBlockMetadata(x, y, z);

                                if (mc.theWorld.getBlock(x, y, z)
                                    .hasTileEntity()) {
                                    meta = 0;
                                }

                                BlockInfo found = null;

                                for (OreInfo ore : OresSearch.searchList) // Now we're actually checking if the current
                                                                          // x,y,z block is in our searchList.
                                {
                                    if ((ore.draw) && (id == ore.id) && (meta == ore.meta)) // Dont check meta if its -1
                                                                                            // (custom)
                                    {
                                        found = new BlockInfo(x, y, z, ore.color); // Add this block to the temp list
                                        break; // Found a match, move on to the next block.
                                    }
                                }

                                // yeah, lol, I'm doing this fork to find the fucking end portal
                                if (block == Blocks.end_portal_frame) found = new BlockInfo(x, y, z, 0x00FF00);

                                if (found != null) {
                                    // Add this block to the temp list
                                    temp.add(found);
                                }
                            }
                        }
                    }
                    RenderTick.ores.clear();
                    RenderTick.ores.addAll(temp); // Add all our found blocks to the RenderTick.ores list. To be use by
                                                  // RenderTick when drawing.
                    nextTimeMs = System.currentTimeMillis() + delayMs; // Update the delay.
                } else {
                    this.thread.interrupt(); // Kill the thread if we turn off xray or the player/world object becomes
                                             // null.
                }
            }
            // System.out.println(" --- Thread Exited Cleanly! ");
            this.thread = null;
        } catch (Exception exc) {
            System.out.println(" ClientTick Thread Interrupted!!! " + exc); // This shouldnt get called.
        }
    }

}
