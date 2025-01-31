package com.cleanroommc.multiblocked.api.tile.part;

import com.cleanroommc.multiblocked.Multiblocked;
import com.cleanroommc.multiblocked.api.capability.trait.CapabilityTrait;
import com.cleanroommc.multiblocked.api.capability.trait.InterfaceUser;
import com.cleanroommc.multiblocked.api.crafttweaker.interfaces.ICTPart;
import com.cleanroommc.multiblocked.api.definition.ComponentDefinition;
import com.cleanroommc.multiblocked.api.definition.PartDefinition;
import com.cleanroommc.multiblocked.api.pattern.MultiblockState;
import com.cleanroommc.multiblocked.api.tile.ComponentTileEntity;
import com.cleanroommc.multiblocked.api.tile.ControllerTileEntity;
import com.cleanroommc.multiblocked.client.renderer.IRenderer;
import com.cleanroommc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * A TileEntity that defies the part of multi.
 *
 * part of the multiblock.
 */
@Optional.Interface(modid = Multiblocked.MODID_CT, iface = "com.cleanroommc.multiblocked.api.crafttweaker.interfaces.ICTPart")
public abstract class PartTileEntity<T extends PartDefinition> extends ComponentTileEntity<T> implements ICTPart {

    public Set<BlockPos> controllerPos = new HashSet<>();

    @Override
    public PartTileEntity<?> getInner() {
        return this;
    }

    @Override
    public boolean isFormed() {
        for (BlockPos blockPos : controllerPos) {
            TileEntity controller = world.getTileEntity(blockPos);
            if (controller instanceof ControllerTileEntity && ((ControllerTileEntity) controller).isFormed()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IRenderer updateCurrentRenderer() {
        if (definition.dynamicRenderer != null) {
            try {
                return definition.dynamicRenderer.apply(this);
            } catch (Exception exception) {
                definition.dynamicRenderer = null;
                Multiblocked.LOGGER.error("definition {} custom logic {} error", definition.location, "dynamicRenderer", exception);
            }
        }
        if (definition.workingRenderer != null) {
            for (ControllerTileEntity controller : getControllers()) {
                if (controller.isFormed() && controller.getStatus().equals("working")) {
                    return definition.workingRenderer;
                }
            }
        }
        return super.updateCurrentRenderer();
    }

    public boolean canShared() {
        return definition.canShared;
    }
    
    public List<ControllerTileEntity> getControllers() {
        List<ControllerTileEntity> result = new ArrayList<>();
        for (BlockPos blockPos : controllerPos) {
            TileEntity controller = world.getTileEntity(blockPos);
            if (controller instanceof ControllerTileEntity && ((ControllerTileEntity) controller).isFormed()) {
                result.add((ControllerTileEntity) controller);
            }
        }
        return result;
    }

    public void addedToController(@Nonnull ControllerTileEntity controller){
        if (controllerPos.add(controller.getPos())) {
            writeCustomData(-1, this::writeControllersToBuffer);
            if (definition.partAddedToMulti != null) {
                try {
                    definition.partAddedToMulti.apply(this, controller);
                } catch (Exception exception) {
                    definition.partAddedToMulti = null;
                    Multiblocked.LOGGER.error("definition {} custom logic {} error", definition.location, "partAddedToMulti", exception);
                }
            }
            setStatus("idle");
        }
    }

    public void removedFromController(@Nonnull ControllerTileEntity controller){
        if (controllerPos.remove(controller.getPos())) {
            writeCustomData(-1, this::writeControllersToBuffer);
            if (definition.partRemovedFromMulti != null) {
                try {
                    definition.partRemovedFromMulti.apply(this, controller);
                } catch (Exception exception) {
                    definition.partRemovedFromMulti = null;
                    Multiblocked.LOGGER.error("definition {} custom logic {} error", definition.location, "partRemovedFromMulti", exception);
                }
            }
            if (getControllers().isEmpty()) {
                setStatus("unformed");
            }
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        writeControllersToBuffer(buf);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        readControllersFromBuffer(buf);
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        if (dataId == -1) {
            readControllersFromBuffer(buf);
        } else {
            super.receiveCustomData(dataId, buf);
        }
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        super.readFromNBT(compound);
        for (MultiblockState state : MultiblockWorldSavedData.getOrCreate(world).getControllerInChunk(new ChunkPos(pos))) {
            if(state.isPosInCache(pos)) {
                controllerPos.add(state.controllerPos);
            }
        }
    }

    private void writeControllersToBuffer(PacketBuffer buffer) {
        buffer.writeVarInt(controllerPos.size());
        for (BlockPos pos : controllerPos) {
            buffer.writeBlockPos(pos);
        }
    }

    private void readControllersFromBuffer(PacketBuffer buffer) {
        int size = buffer.readVarInt();
        controllerPos.clear();
        for (int i = size; i > 0; i--) {
            controllerPos.add(buffer.readBlockPos());
        }
    }

    public static class PartSimpleTileEntity extends PartTileEntity<PartDefinition> {

    }

}
