package github.kasuminova.ecoaeextension.common.block.ecotech.ecalculator;

import github.kasuminova.ecoaeextension.ECOAEExtension;
import github.kasuminova.ecoaeextension.common.CommonProxy;
import github.kasuminova.ecoaeextension.common.core.CreativeTabNovaEng;
import github.kasuminova.ecoaeextension.common.tile.ecotech.ecalculator.ECalculatorController;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class BlockECalculatorController extends BlockController {
    public static final Map<ResourceLocation, BlockECalculatorController> REGISTRY = new LinkedHashMap<>();
    public static final BlockECalculatorController L4;
    public static final BlockECalculatorController L6;
    public static final BlockECalculatorController L9;

    static {
        L4 = new BlockECalculatorController("l4");
        REGISTRY.put(L4.registryName, L4);
        L6 = new BlockECalculatorController("l6");
        REGISTRY.put(L6.registryName, L6);
        L9 = new BlockECalculatorController("l9");
        REGISTRY.put(L9.registryName, L9);
    }

    protected final ResourceLocation registryName;
    protected final ResourceLocation machineRegistryName;

    public BlockECalculatorController(final String level) {
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setHardness(20.0F);
        this.setResistance(2000.0F);
        this.fullBlock = false;

        registryName = new ResourceLocation(ECOAEExtension.MOD_ID, "extendable_calculator_subsystem_" + level);
        machineRegistryName = new ResourceLocation(ModularMachinery.MODID, registryName.getPath());
        setRegistryName(registryName);
        setTranslationKey(ECOAEExtension.MOD_ID + '.' + registryName.getPath());
    }

    @Nonnull
    public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            return super.getActualState(state, worldIn, pos);
        }
        return state;
    }

    @Override
    public int getLightValue(@Nonnull final IBlockState state) {
        return state.getValue(FORMED) ? 12 : 4;
    }

    @Override
    @Nonnull
    public IBlockState getStateForPlacement(
            @Nonnull final World world,
            @Nonnull final BlockPos pos,
            @Nonnull final EnumFacing facing,
            final float hitX,
            final float hitY,
            final float hitZ,
            final int meta,
            @Nullable final EntityLivingBase placer,
            @Nonnull final EnumHand hand
    ) {
        IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand);
        EnumFacing resolvedFacing = resolvePlacementFacing(placer);
        state = state.withProperty(FACING, resolvedFacing);
        logPlacementDebug("getStateForPlacement", world, pos, placer, hitX, hitY, hitZ, resolvedFacing, state.getValue(FACING));
        return state;
    }

    @Override
    public void onBlockPlacedBy(
            @Nonnull final World worldIn,
            @Nonnull final BlockPos pos,
            @Nonnull final IBlockState state,
            @Nullable final EntityLivingBase placer,
            @Nonnull final ItemStack stack
    ) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        EnumFacing placementFacing = resolvePlacementFacing(placer);
        IBlockState currentState = worldIn.getBlockState(pos);
        if (currentState.getPropertyKeys().contains(FACING) && currentState.getValue(FACING) != placementFacing) {
            currentState = currentState.withProperty(FACING, placementFacing);
            worldIn.setBlockState(pos, currentState, 3);
        }
        logPlacementDebug("onBlockPlacedBy.afterFix", worldIn, pos, placer, 0F, 0F, 0F, placementFacing,
                currentState.getPropertyKeys().contains(FACING) ? currentState.getValue(FACING) : null);

        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof ECalculatorController ctrl) {
            ctrl.setPlacementFacingLock(placementFacing);
            if (!worldIn.isRemote) {
                ctrl.notifyStructureFormedState(ctrl.isStructureFormed());
            }
        }
    }

    @Nonnull
    protected EnumFacing resolvePlacementFacing(@Nullable final EntityLivingBase placer) {
        if (placer == null) {
            return EnumFacing.NORTH;
        }
        return placer.getHorizontalFacing().getOpposite();
    }

    protected void logPlacementDebug(
            @Nonnull final String stage,
            @Nonnull final World world,
            @Nonnull final BlockPos pos,
            @Nullable final EntityLivingBase placer,
            final float hitX,
            final float hitY,
            final float hitZ,
            @Nonnull final EnumFacing resolvedFacing,
            @Nullable final EnumFacing stateFacing
    ) {
        ECOAEExtension.log.info(
                "[ECOAE][FacingLock] stage={} side={} dim={} pos={} placer={} yaw={} pitch={} hit=({},{},{}) resolvedFacing={} stateFacing={}",
                stage,
                world.isRemote ? "CLIENT" : "SERVER",
                world.provider.getDimension(),
                pos,
                placer == null ? "null" : placer.getName(),
                placer == null ? 0F : placer.rotationYaw,
                placer == null ? 0F : placer.rotationPitch,
                hitX,
                hitY,
                hitZ,
                resolvedFacing,
                stateFacing
        );
    }

    @Override
    public boolean onBlockActivated(final World worldIn, @Nonnull final BlockPos pos, @Nonnull final IBlockState state, @Nonnull final EntityPlayer playerIn, @Nonnull final EnumHand hand, @Nonnull final EnumFacing facing, final float hitX, final float hitY, final float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof ECalculatorController controller && controller.isStructureFormed()) {
                playerIn.openGui(ECOAEExtension.MOD_ID, CommonProxy.GuiType.ECALCULATOR_CONTROLLER.ordinal(), worldIn, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    public DynamicMachine getParentMachine() {
        return MachineRegistry.getRegistry().getMachine(machineRegistryName);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final World world, final IBlockState state) {
        return new ECalculatorController(machineRegistryName);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(final World worldIn, final int meta) {
        return new ECalculatorController(machineRegistryName);
    }

}
