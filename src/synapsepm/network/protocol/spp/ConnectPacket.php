<?php
declare(strict_types=1);

namespace synapsepm\network\protocol\spp;

class ConnectPacket extends DataPacket {
    const NETWORK_ID = SynapseInfo::CONNECT_PACKET;

    public $protocol = SynapseInfo::CURRENT_PROTOCOL;
    public $maxPlayers;
    public $islobbyServer;
    public $transferOnShutdown;
    public $description;
    public $password;

    public function encode() {
        $this->reset();
        $this->putInt($this->protocol);
        $this->putInt($this->maxPlayers);
        $this->putBool($this->islobbyServer);
        $this->putBool($this->transferOnShutdown);
        $this->putString($this->description);
        $this->putString($this->password);
    }

    public function decode() {
        $this->protocol = $this->getInt();
        $this->maxPlayers = $this->getInt();
        $this->islobbyServer = $this->getBool();
        $this->transferOnShutdown = $this->getBool();
        $this->description = $this->getString();
        $this->password = $this->getString();
    }
}