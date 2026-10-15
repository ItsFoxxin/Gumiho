param([Parameter(Mandatory = $true)][string]$JavaHome)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$outDir = Join-Path $projectRoot 'build/combat-regression'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$sources = @(
    'src/main/java/dev/gumiho/client/gui/HudSizing.java',
    'src/test/java/dev/gumiho/client/module/utility/HudSizingRegressionTest.java',
    'src/main/java/dev/gumiho/client/module/movement/BridgeRules.java',
    'src/test/java/dev/gumiho/client/module/movement/BridgeRegressionTest.java',
    'src/main/java/dev/gumiho/client/module/utility/HudValues.java',
    'src/test/java/dev/gumiho/client/module/utility/VisualUtilityRegressionTest.java',
    'src/main/java/dev/gumiho/client/gui/HudLayout.java',
    'src/main/java/dev/gumiho/client/gui/CombatSection.java',
    'src/main/java/dev/gumiho/client/gui/CombatOptionHelp.java',
    'src/main/java/dev/gumiho/client/gui/DescriptionScroll.java',
    'src/main/java/dev/gumiho/client/module/utility/NetworkStatus.java',
    'src/test/java/dev/gumiho/client/module/utility/UiUtilityRegressionTest.java',
    'src/main/java/dev/gumiho/client/module/combat/ShieldAttackWindow.java',
    'src/test/java/dev/gumiho/client/module/combat/ShieldWindowRegressionTest.java',
    'src/main/java/dev/gumiho/client/module/combat/ShieldThreatRules.java',
    'src/main/java/dev/gumiho/client/module/combat/RangedWeaponRules.java',
    'src/main/java/dev/gumiho/client/module/combat/RangedBallistics.java',
    'src/test/java/dev/gumiho/client/module/combat/RangedRegressionTest.java',
    'src/main/java/dev/gumiho/client/module/utility/TrajectoryPhysics.java',
    'src/test/java/dev/gumiho/client/module/utility/TrajectoryRegressionTest.java',
    'src/main/java/dev/gumiho/client/module/combat/CombatRules.java',
    'src/main/java/dev/gumiho/client/module/combat/AttackGate.java',
    'src/main/java/dev/gumiho/client/module/combat/EquipmentSnapshot.java',
    'src/main/java/dev/gumiho/client/module/combat/AimSmoothing.java',
    'src/main/java/dev/gumiho/client/module/combat/StableTarget.java',
    'src/main/java/dev/gumiho/client/module/combat/ImpactGuard.java',
    'src/main/java/dev/gumiho/client/module/combat/MaceFlight.java',
    'src/main/java/dev/gumiho/client/module/utility/LaunchMath.java',
    'src/test/java/dev/gumiho/client/module/combat/ResetPreviewRegressionTest.java',
    'src/main/java/dev/gumiho/client/module/combat/WindSwapTrigger.java',
    'src/main/java/dev/gumiho/client/config/GumihoConfig.java',
    'src/main/java/dev/gumiho/client/config/CombatConfigMigration.java',
    'src/test/java/dev/gumiho/client/module/combat/CombatRegressionTest.java'
) | ForEach-Object { Join-Path $projectRoot $_ }
& (Join-Path $JavaHome 'bin/javac.exe') -encoding UTF-8 -d $outDir $sources
if ($LASTEXITCODE -ne 0) { throw 'Combat test compilation failed' }
& (Join-Path $JavaHome 'bin/java.exe') -cp $outDir dev.gumiho.client.module.combat.CombatRegressionTest
if ($LASTEXITCODE -ne 0) { throw 'Combat regression tests failed' }
