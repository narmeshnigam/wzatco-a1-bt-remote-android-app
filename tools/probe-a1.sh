#!/usr/bin/env bash
#
# Read-only survey of the WZATCO A1's input stack, for open question 18.
#
# Walking usages from the phone only ever shows what the projector does *not* answer to. This
# reads the projector's own side: which input devices it has, which key layout files map which
# scancode to which Android keycode, and — in watch mode — exactly what arrives when the phone
# sends a key. That is the difference between "no code works" and "no code we can send works".
#
# Nothing here writes to the projector. The keycode probe that *does* act on it is a separate
# step, described in docs/HARDWARE_RUNBOOK.md, because it changes projector state.
#
# Usage:
#   tools/probe-a1.sh survey [serial]   # dump the input stack to a directory (default)
#   tools/probe-a1.sh watch  [serial]   # live: print key events as they arrive, ctrl-C to stop
#
set -uo pipefail

ADB=${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}
command -v "$ADB" >/dev/null 2>&1 || ADB=adb

MODE=${1:-survey}
SERIAL=${2:-}

# macOS ships bash 3.2, where expanding an empty array under `set -u` is an error, so the serial
# is threaded through a function rather than an array of arguments.
adb_() {
    if [ -n "$SERIAL" ]; then "$ADB" -s "$SERIAL" "$@"; else "$ADB" "$@"; fi
}
sh_() { adb_ shell "$@" 2>&1; }

if ! adb_ get-state >/dev/null 2>&1; then
    echo "No device. Connect the projector's USB data port to this machine, enable USB debugging" >&2
    echo "on the projector, and accept the RSA prompt on the projected image. Then:" >&2
    echo "  $ADB devices -l" >&2
    exit 1
fi

model=$(sh_ getprop ro.product.model | tr -d '\r')
echo "target: ${SERIAL:-<only device>}  model: $model"
case "$model" in
    DN2101*) echo "REFUSING: that is the phone, not the projector. Pass the projector's serial." >&2; exit 2 ;;
esac

case "$MODE" in
survey)
    OUT="a1-probe-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$OUT"
    echo "writing to $OUT/"

    {
        echo "# identity"
        for p in ro.product.model ro.product.brand ro.product.board ro.build.version.release \
                 ro.build.version.sdk ro.build.fingerprint; do
            printf '%-32s %s\n' "$p" "$(sh_ getprop "$p" | tr -d '\r')"
        done
    } > "$OUT/identity.txt"

    # Input devices and every key each one can emit. The phone appears here once connected, and
    # its key list is exactly what our HID descriptor declares.
    sh_ getevent -lp > "$OUT/input-devices.txt"

    # The scancode -> Android keycode tables. This is the authoritative answer to "is focus even
    # bound to a key on this firmware, and to which one".
    sh_ ls -la /system/usr/keylayout/ > "$OUT/keylayout-list.txt"
    for kl in $(sh_ ls /system/usr/keylayout/ | tr -d '\r'); do
        {
            echo "===== /system/usr/keylayout/$kl ====="
            sh_ cat "/system/usr/keylayout/$kl"
        } >> "$OUT/keylayouts.txt"
    done
    sh_ ls -la /system/usr/idc/ > "$OUT/idc-list.txt" 2>/dev/null

    # What the input system believes about each device, including which layout it loaded.
    sh_ dumpsys input > "$OUT/dumpsys-input.txt"

    # Anything vendor-side that might own focus, so the search has somewhere to go if no key
    # binding exists at all.
    sh_ pm list packages > "$OUT/packages.txt"

    {
        echo "# lines mentioning focus / zoom / keystone / flip in the key layouts"
        grep -inE "focus|zoom|keystone|flip" "$OUT/keylayouts.txt" || echo "(no matches)"
        echo
        echo "# packages whose name suggests projector control"
        grep -inE "focus|projector|keystone|hisilicon|hisense|mstar|optical" "$OUT/packages.txt" || echo "(no matches)"
    } > "$OUT/summary.txt"

    echo
    cat "$OUT/summary.txt"
    echo
    echo "Full dump in $OUT/. The file that settles question 18 is keylayouts.txt."
    ;;

watch)
    echo "Watching key events. Press Focus + on the phone now; ctrl-C when done."
    echo "A line here means the usage reaches the projector. No line means it never arrives."
    adb_ shell getevent -lt | grep --line-buffered -E "EV_KEY|EV_MSC"
    ;;

*)
    echo "usage: $0 [survey|watch] [serial]" >&2
    exit 64
    ;;
esac
