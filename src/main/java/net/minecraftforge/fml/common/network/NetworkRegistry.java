//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.minecraftforge.fml.common.network;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.netty.channel.ChannelHandler;
import io.netty.util.AttributeKey;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.network.INetHandler;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.network.FMLOutboundHandler.OutboundTarget;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.common.network.internal.NetworkModHolder;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public enum NetworkRegistry {
    INSTANCE;

    private EnumMap<Side, Map<String, FMLEmbeddedChannel>> channels = Maps.newEnumMap(Side.class);
    private Map<ModContainer, NetworkModHolder> registry = Maps.newHashMap();
    private Map<ModContainer, IGuiHandler> serverGuiHandlers = Maps.newHashMap();
    private Map<ModContainer, IGuiHandler> clientGuiHandlers = Maps.newHashMap();
    public static final AttributeKey<String> FML_CHANNEL = AttributeKey.valueOf("fml:channelName");
    public static final AttributeKey<Side> CHANNEL_SOURCE = AttributeKey.valueOf("fml:channelSource");
    public static final AttributeKey<ModContainer> MOD_CONTAINER = AttributeKey.valueOf("fml:modContainer");
    public static final AttributeKey<INetHandler> NET_HANDLER = AttributeKey.valueOf("fml:netHandler");
    public static final AttributeKey<Boolean> FML_MARKER = AttributeKey.valueOf("fml:hasMarker");
    public static final byte FML_PROTOCOL = 2;

    private NetworkRegistry() {
        this.channels.put(Side.CLIENT, Maps.<String,FMLEmbeddedChannel>newConcurrentMap());
        this.channels.put(Side.SERVER, Maps.<String,FMLEmbeddedChannel>newConcurrentMap());
        this.channels.put(Side.BUKKIT, Maps.<String,FMLEmbeddedChannel>newConcurrentMap());
    }

    public EnumMap<Side, FMLEmbeddedChannel> newChannel(String name, ChannelHandler... handlers) {
        if (!((Map)this.channels.get(Side.CLIENT)).containsKey(name) && !((Map)this.channels.get(Side.SERVER)).containsKey(name) && !name.startsWith("MC|") && !name.startsWith("\u0001") && !name.startsWith("FML")) {
            EnumMap<Side, FMLEmbeddedChannel> result = Maps.newEnumMap(Side.class);
            Side[] var4 = Side.values();
            int var5 = var4.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                Side side = var4[var6];
                FMLEmbeddedChannel channel = new FMLEmbeddedChannel(name, side, handlers);
                ((Map)this.channels.get(side)).put(name, channel);
                result.put(side, channel);
            }

            return result;
        } else {
            throw new RuntimeException("That channel is already registered");
        }
    }

    public SimpleNetworkWrapper newSimpleChannel(String name) {
        return new SimpleNetworkWrapper(name);
    }

    public FMLEventChannel newEventDrivenChannel(String name) {
        return new FMLEventChannel(name);
    }

    public EnumMap<Side, FMLEmbeddedChannel> newChannel(ModContainer container, String name, ChannelHandler... handlers) {
        if (((Map)this.channels.get(Side.CLIENT)).containsKey(name) || ((Map)this.channels.get(Side.SERVER)).containsKey(name) || name.startsWith("MC|") || name.startsWith("\u0001") || name.startsWith("FML") && !"FML".equals(container.getModId())) {
            throw new RuntimeException("That channel is already registered");
        } else {
            EnumMap<Side, FMLEmbeddedChannel> result = Maps.newEnumMap(Side.class);
            Side[] var5 = Side.values();
            int var6 = var5.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                Side side = var5[var7];
                FMLEmbeddedChannel channel = new FMLEmbeddedChannel(container, name, side, handlers);
                ((Map)this.channels.get(side)).put(name, channel);
                result.put(side, channel);
            }

            return result;
        }
    }

    public FMLEmbeddedChannel getChannel(String name, Side source) {
        return (FMLEmbeddedChannel)((Map)this.channels.get(source)).get(name);
    }

    public void registerGuiHandler(Object mod, IGuiHandler handler) {
        ModContainer mc = FMLCommonHandler.instance().findContainerFor(mod);
        if (mc == null) {
            FMLLog.log.error("Mod of type {} attempted to register a gui network handler during a construction phase", mod.getClass().getName());
            throw new RuntimeException("Invalid attempt to create a GUI during mod construction. Use an EventHandler instead");
        } else {
            this.serverGuiHandlers.put(mc, handler);
            this.clientGuiHandlers.put(mc, handler);
        }
    }

    @Nullable
    public Container getRemoteGuiContainer(ModContainer mc, EntityPlayerMP player, int modGuiId, World world, int x, int y, int z) {
        IGuiHandler handler = (IGuiHandler)this.serverGuiHandlers.get(mc);
        return handler != null ? (Container)handler.getServerGuiElement(modGuiId, player, world, x, y, z) : null;
    }

    @Nullable
    public Object getLocalGuiContainer(ModContainer mc, EntityPlayer player, int modGuiId, World world, int x, int y, int z) {
        IGuiHandler handler = (IGuiHandler)this.clientGuiHandlers.get(mc);
        return handler.getClientGuiElement(modGuiId, player, world, x, y, z);
    }

    public boolean hasChannel(String channelName, Side source) {
        return ((Map)this.channels.get(source)).containsKey(channelName);
    }

    public void register(ModContainer fmlModContainer, Class<?> clazz, @Nullable String remoteVersionRange, ASMDataTable asmHarvestedData) {
        NetworkModHolder networkModHolder = new NetworkModHolder(fmlModContainer, clazz, remoteVersionRange, asmHarvestedData);
        this.registry.put(fmlModContainer, networkModHolder);
        networkModHolder.testVanillaAcceptance();
    }

    public boolean isVanillaAccepted(Side from) {
        return this.registry.values().stream().allMatch((mod) -> {
            return mod.acceptsVanilla(from);
        });
    }

    public Collection<String> getRequiredMods(Side from) {
        return (Collection)this.registry.values().stream().filter((mod) -> {
            return !mod.acceptsVanilla(from);
        }).map((mod) -> {
            return mod.getContainer().getName();
        }).sorted().collect(Collectors.toList());
    }

    public Map<ModContainer, NetworkModHolder> registry() {
        return ImmutableMap.copyOf(this.registry);
    }

    public Set<String> channelNamesFor(Side side) {
        return ((Map)this.channels.get(side)).keySet();
    }

    public void fireNetworkHandshake(NetworkDispatcher networkDispatcher, Side origin) {
        NetworkHandshakeEstablished handshake = new NetworkHandshakeEstablished(networkDispatcher, networkDispatcher.getNetHandler(), origin);
        Iterator var4 = ((Map)this.channels.get(origin)).values().iterator();

        while(var4.hasNext()) {
            FMLEmbeddedChannel channel = (FMLEmbeddedChannel)var4.next();
            channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(OutboundTarget.DISPATCHER);
            channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(networkDispatcher);
            channel.pipeline().fireUserEventTriggered(handshake);
        }

    }

    public void cleanAttributes() {
        this.channels.values().forEach((map) -> {
            map.values().forEach(FMLEmbeddedChannel::cleanAttributes);
        });
    }

    public static class TargetPoint {
        public final double x;
        public final double y;
        public final double z;
        public final double range;
        public final int dimension;

        public TargetPoint(int dimension, double x, double y, double z, double range) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.range = range;
            this.dimension = dimension;
        }
    }
}
