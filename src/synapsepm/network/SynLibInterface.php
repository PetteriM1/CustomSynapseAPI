<?php
declare(strict_types=1);

namespace synapsepm\network;

use pocketmine\network\mcpe\protocol\DataPacket;
use pocketmine\network\SourceInterface;
use pocketmine\Player;
use synapsepm\network\protocol\spp\RedirectPacket;
use synapsepm\Synapse;


class SynLibInterface implements SourceInterface {
    private $synapseInterface;
    private $synapse;

    public function __construct(Synapse $synapse, SynapseInterface $interface) {
        $this->synapse = $synapse;
        $this->synapseInterface = $interface;
    }

    public function start() {

    }

    public function getSynapse(): Synapse {
        return $this->synapse;
    }

    public function emergencyShutdown() {
    }

    public function setName(string $name) {
    }

    public function process(): void {

    }

    public function close(Player $player, string $reason = 'unknown reason') {
    }

    public function putPacket(Player $player, DataPacket $packet, bool $needACK = false, bool $immediate = true) {
        if (!$player->isClosed()) {
            $pk = new RedirectPacket();
            $pk->uuid = $player->getUniqueId();
            $pk->direct = $immediate;
            if (!$packet->isEncoded) {
                $packet->encode();
            }
            $pk->mcpeBuffer = $packet->buffer;
            $this->synapseInterface->putPacket($pk);
        }
        return null;
    }

    public function shutdown() {
    }
}