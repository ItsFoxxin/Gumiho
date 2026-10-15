package dev.gumiho.client.gui;

/** Shared concise descriptions for the existing combat controls. */
public final class CombatOptionHelp {
    public static String text(String label) {
        String key = label.split("\\|", 2)[0].strip();
        return switch (key) {
            case "Hostile Mobs" -> "Allow hostile mobs as targets. Does not enable attacks by itself. | 적대적 몹을 대상으로 허용합니다.";
            case "Marked Enemies" -> "Allow players explicitly marked Enemy. Teammates and neutral players remain protected. | 지정한 적 플레이어만 허용합니다.";
            case "Passive Mobs" -> "Allow passive animals as targets; leave OFF to avoid them. | 비적대적 동물을 대상으로 허용합니다.";
            case "Invisible Targets" -> "Allow invisible eligible targets; line-of-sight and range checks still apply. | 투명 대상도 거리와 시야 검사를 거칩니다.";
            case "Priority" -> "Choose the best eligible target by crosshair angle, distance, or low health. | 조준선, 거리 또는 낮은 체력으로 우선순위를 정합니다.";
            case "Target Range" -> "Maximum target-search distance. Actual weapon reach is still checked separately. | 탐색 거리이며 실제 무기 사거리를 늘리지 않습니다.";
            case "Full FOV" -> "Total targeting cone around your crosshair; 90 means 45 degrees to each side. | 전체 조준 범위입니다.";
            case "Melee Range" -> "Maximum normal melee attack distance, capped by native interaction reach. | 게임이 허용하는 근접 사거리를 넘지 않습니다.";
            case "Melee Aim Speed" -> "Maximum turn speed. Higher values track faster; easing controls how softly aim catches up. | 높을수록 빠르게 조준합니다.";
            case "Aim Ease" -> "Smoothing response time in milliseconds. Higher is softer/slower; lower is snappier. | 높을수록 부드럽고 느립니다.";
            case "Weapons Only" -> "Restrict automatic melee to supported held weapons. Does not enable auto attacks by itself. | 지원 무기로만 자동 근접 공격합니다.";
            case "Wait Hurt Animation" -> "Wait while the target is in its recent-hit animation to avoid wasted attacks. | 대상의 피격 동작이 끝날 때까지 기다립니다.";
            case "Avoid Raised Shield" -> "Avoid attacking into a facing shield; axes retain their shield interaction. | 정면 방패 공격을 피하며 도끼는 예외입니다.";
            case "Detailed Target HUD" -> "Show extra equipment/threat information for the selected target. | 대상의 장비와 위협 정보를 표시합니다.";
            case "Shield Attack Windows" -> "Briefly drop only an auto-raised shield for manual attacks or eligible full-cooldown hits. Imminent projectiles can delay automatic openings. | 공격할 때 자동 방패를 잠시 내립니다.";
            case "Mace Minimum Fall" -> "Required fall distance before automated smash attempts. Also requires descent, reach and native smash readiness; no fall-damage bypass. | 강타를 시도할 최소 낙하 거리입니다.";
            case "Mace Lock Range" -> "Search distance for mace landing/reset targets, not extended attack reach. | 착지 대상을 찾는 거리이며 공격 사거리는 아닙니다.";
            case "Landing Guide" -> "Show estimated landing and target/reset guidance with Mace Combo Assist. | 예상 착지와 연속 공격 안내를 표시합니다.";
            case "Mace Air Correction" -> "Opt-in normal movement-input correction toward a reachable mace target. Your movement input takes priority. | 공중 이동을 보정하며 수동 입력이 우선합니다.";
            case "Reset Radius" -> "How close a potential target must be to the predicted landing point. | 예상 착지 지점 주변의 대상 탐색 반경입니다.";
            case "Shield Lead" -> "How early to raise the shield before a predicted impact, in ticks. Native shield startup still applies. | 충돌 전에 방패를 올릴 준비 시간입니다.";
            case "Shield Projectiles" -> "Predict supported incoming blockable projectiles. Piercing/bypass attacks and latency can defeat the shield. | 방어 가능한 투사체를 예측합니다.";
            case "React to Swings" -> "React to a new nearby frontal swing. A swing is not proof that damage will land. | 가까운 정면 휘두르기에 반응합니다.";
            case "Close Melee Guard" -> "Guard against nearby frontal melee threats; crowds may keep the shield raised. | 가까운 근접 위협에 방어 자세를 유지합니다.";
            case "Explosion Guard" -> "Estimate nearby primed TNT/creeper threats. Not every explosion or damage type is shield-blockable. | TNT와 크리퍼 위협을 예측하지만 완전 방어는 아닙니다.";
            default -> "Adjust this option | 옵션을 조절합니다";
        };
    }
    private CombatOptionHelp() { }
}
