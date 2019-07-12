<?php
/**
 * @author CreeperFace
 */

declare(strict_types=1);

namespace synapsepm\network\protocol\spp;

class PluginMessagePacket extends DataPacket {

    const NETWORK_ID = SynapseInfo::PLUGIN_MESSAGE_PACKET;

    public $channel;
    public $data;

    public function encode() {
        $this->reset();
        $this->putString($this->channel);
        $this->putString($this->data);
    }

    public function decode() {
        $this->channel = $this->getString();
        $this->data = $this->getString();
    }
}