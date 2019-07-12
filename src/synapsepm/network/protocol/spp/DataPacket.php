<?php
declare(strict_types=1);

namespace synapsepm\network\protocol\spp;

use pocketmine\network\mcpe\NetworkBinaryStream;


abstract class DataPacket extends NetworkBinaryStream {

    const NETWORK_ID = 0;

    public function pid(): int {
        return $this::NETWORK_ID;
    }

    abstract public function encode();

    abstract public function decode();

    public function reset() {
        $this->buffer = chr($this::NETWORK_ID);
        $this->offset = 0;
    }
}