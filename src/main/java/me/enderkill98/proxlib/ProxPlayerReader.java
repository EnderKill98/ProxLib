package me.enderkill98.proxlib;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

public class ProxPlayerReader {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProxLib/PlayerReader");

    private PlayerEntity player;
    private int magicBytesPos = 0;
    private @Nullable BlockPos assumedPlayerEyeBlockPos = null;
    private ProxDataUnitReader dataReader = null;
    private Pair<Integer/*Length (3 byte)*/, @Nullable Short/*Id (2 byte)*/> dataHeader = null;
    private long lastReceivedAt = -1L;
    private ArrayList<ProxPacketReceiveHandler> handlers = new ArrayList<>();

    public ProxPlayerReader(PlayerEntity player) {
        this.player = player;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public void addHandler(ProxPacketReceiveHandler handler) {
        handlers.add(handler);
    }

    public void handle(BlockBreakingProgressS2CPacket packet) {
        final long now = System.currentTimeMillis();
        if (lastReceivedAt != -1L && now - lastReceivedAt > 3000) {
            magicBytesPos = 0;
            assumedPlayerEyeBlockPos = null;
            dataReader = null;
            dataHeader = null;
        }
        lastReceivedAt = now;

        if (packet.getProgress() != 255) return; // We only care about ABORT_BLOCK_BREAKs
        if (packet.getEntityId() != player.getId()) return; // Not for this player

        // Mid-MagicByte parsing
        if (dataReader == null && assumedPlayerEyeBlockPos != null && magicBytesPos > 0 && magicBytesPos < ProxPackets.PACKET_PDU_MAGIC.length) {
            // Currently, with a 2 PDU-long magic, this will always mean magicBytePos == 1
            int expectedPdu = ProxPackets.PACKET_PDU_MAGIC[magicBytesPos];
            int actualPdu = ProxDataUnits.blockPosToProxDataUnit(assumedPlayerEyeBlockPos, packet.getPos());
            //LOGGER.info("Mid-MagicBytes pos: " + magicBytesPos + ", expected PDU: " + expectedPdu + ", actual PDU: " + actualPdu);
            if (expectedPdu == actualPdu) {
                magicBytesPos++;
                if (magicBytesPos == ProxPackets.PACKET_PDU_MAGIC.length) {
                    // Full magic received!
                    dataReader = new ProxDataUnitReader();
                    dataHeader = null;
                    return; // Do not process this as a data pdu later on
                }
            } else {
                // Reset
                magicBytesPos = 0;
                assumedPlayerEyeBlockPos = null;
            }
        }

        // Start MagicByte parsing
        if(dataReader == null && magicBytesPos == 0) {
            BlockPos firstByteOffset = ProxDataUnits.ALL_OFFSETS[ProxPackets.PACKET_PDU_MAGIC[0]];
            // Blindly assume that player sent this byte on purpose and therefore has a corresponding EyeBlockPos
            assumedPlayerEyeBlockPos = packet.getPos().subtract(firstByteOffset);
            magicBytesPos++;
            //LOGGER.info("Start-MagicBytes pos: " + magicBytesPos + ", assumedPlayerEyeBlockPos: " + assumedPlayerEyeBlockPos);
        }

        if(assumedPlayerEyeBlockPos == null || dataReader == null) return; // MagicBytes not successfully received yet

        int pdu = ProxDataUnits.blockPosToProxDataUnit(assumedPlayerEyeBlockPos, packet.getPos());
        //LOGGER.info("Data PDU: " + pdu);
        if(pdu == -1) return; // Invalid offset (maybe player moved too much?)

        if(pdu >= ProxDataUnits.getMaxUsableProxDataUnit()) {
            // Those are meant for use by MagicBytes only. Consider this an error and reset
            dataReader = null;
            dataHeader = null;
            return;
        }

        magicBytesPos = 0;
        if(dataReader == null)
            return; // Not expecting any data rn. Ignoring
        dataReader.read(pdu);

        final int totalBytes = dataReader.getTotalBytes();
        if(totalBytes >= 3 && dataHeader == null) {
            // Got enough data to figure out expected length
            byte[] bytes = dataReader.getBytes();
            // If not "& 0xFF"'ing, any byte with the highest bit in a bight can make the whole Integer negative for some reason!!!!!!!!
            int expectedLength = ((bytes[0] & 0xFF) << 16) | ((bytes[1] & 0xFF) << 8) | (bytes[2] & 0xFF);
            dataHeader = new Pair<>(expectedLength, null);
        }else if(totalBytes >= 5 && dataHeader != null && dataHeader.getRight() == null) {
            // Got enough data to figure out the id
            byte[] bytes = dataReader.getBytes();
            short id = (short) (bytes[3+0] << 8 | bytes[3+1]);
            dataHeader.setRight(id);
        }else if(dataHeader != null && dataHeader.getRight() != null && totalBytes >= 3+dataHeader.getLeft()) {
            // All data got read
            int expectedLength = dataHeader.getLeft();
            @Nullable Short packedId = dataHeader.getRight();
            if(expectedLength < 2 || packedId == null) {
                LOGGER.info("Packet received from " + player.getGameProfile().getName() + " was too small (length was: " + expectedLength + " and packed Id " + packedId + ")!");
                dataReader = null;
                dataHeader = null;
                return;
            }

            byte[] data = Arrays.copyOfRange(dataReader.getBytes(), 5, expectedLength+3);
            ProxPacketIdentifier identifier = ProxPacketIdentifier.ofPacked(packedId);
            LOGGER.info("Received a prox packet with vendor id " + identifier.vendorId() + ", packet id " + identifier.packetId() + " and " + data.length + " bytes of data.");
            for(ProxPacketReceiveHandler handler : handlers) {
                try {
                    handler.onReceived(identifier, data);
                }catch (Exception ex) {
                    LOGGER.error("Failed when running some handler!", ex);
                }
            }

            // Done
            dataReader = null;
            dataHeader = null;
        }
    }
}
