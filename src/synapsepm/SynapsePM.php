<?php
declare(strict_types=1);

namespace synapsepm;

use pocketmine\network\mcpe\protocol\types\RuntimeBlockMapping;
use pocketmine\plugin\PluginBase;
use synapsepm\command\TransferCommand;
use synapsepm\utils\Utils;


class SynapsePM extends PluginBase {
    /** @var Synapse[] */
    private $synapses = [];
    /** @var bool */
    private $useLoadingScreen;

    public function onLoad() {
        @RuntimeBlockMapping::fromStaticRuntimeId(0);
        Utils::initBlockRuntimeIdMapping();
    }

    public function onEnable() {
        $this->saveDefaultConfig();
        $this->reloadConfig();

        $cfg = $this->getConfig();

        if (!$cfg->get('enable')) {
            $this->setEnabled(false);
            return;
        }

        if ($cfg->get('disable-rak')) {
            $this->getServer()->getPluginManager()->registerEvents(new DisableRakListener(), $this);
        }

        foreach ($this->getConfig()->get('entries') as $synapseConfig) {
            $this->addSynapse(new Synapse($this, $synapseConfig));
        }

        $this->getServer()->getCommandMap()->register("stransfer", new TransferCommand());
    }

    public function onDisable() {
        foreach ($this->synapses as $synapse) {
            $synapse->shutdown();
        }
    }

    /**
     * Add the synapse to the synapses list
     *
     * @param Synapse $synapse
     */
    public function addSynapse(Synapse $synapse) {
        $this->synapses[spl_object_hash($synapse)] = $synapse;
    }

    /**
     * Remove the synapse from the synapses list
     *
     * @param Synapse $synapse
     */
    public function removeSynapse(Synapse $synapse) {
        unset($this->synapses[spl_object_hash($synapse)]);
    }

    /**
     * Return array of the synapses
     * @return Synapse[]
     */
    public function getSynapses(): array {
        return $this->synapses;
    }

    /**
     * @return boolean
     */
    public function isUseLoadingScreen(): bool {
        return $this->useLoadingScreen;
    }
}