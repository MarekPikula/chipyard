package chipyard

import chisel3._

import org.chipsalliance.cde.config.{Config}

// ---------------------
// ARA Configs
// ---------------------

class ARAConfig extends Config(
  new ara.WithNARACores(1) ++
  new chipyard.config.AbstractConfig)

class ARAConfigOpti extends Config(
  new ara.WithNARACores(1) ++
  new freechips.rocketchip.subsystem.WithoutTLMonitors ++
  new chipyard.config.AbstractConfig)

class ARAConfigTrace extends Config(
  new ara.WithNARACores(1, true) ++
  new chipyard.config.AbstractConfig)

class ARAConfigTraceOpti extends Config(
  new ara.WithNARACores(1, true) ++
  new freechips.rocketchip.subsystem.WithoutTLMonitors ++
  new chipyard.config.AbstractConfig)
