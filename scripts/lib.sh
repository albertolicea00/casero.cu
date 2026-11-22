# Shared helpers for the scripts in this folder. Source it, don't run it.

if [ -t 2 ]; then RED=$'\033[31m'; YELLOW=$'\033[33m'; GREEN=$'\033[32m'; BOLD=$'\033[1m'; RESET=$'\033[0m'; else RED=; YELLOW=; GREEN=; BOLD=; RESET=; fi

# fail "what went wrong" "how to fix it" — prints both and exits.
fail() {
    printf '\n%s✗ %s%s\n' "$RED$BOLD" "$1" "$RESET" >&2
    [ -n "${2:-}" ] && printf '%s→ %s%s\n' "$YELLOW" "$2" "$RESET" >&2
    printf '\n' >&2
    exit 1
}

warn() { printf '%s! %s%s\n' "$YELLOW" "$1" "$RESET" >&2; }
step() { printf '%s▸ %s%s\n' "$BOLD" "$1" "$RESET"; }
done_() { printf '%s✓ %s%s\n' "$GREEN" "$1" "$RESET"; }

# prop <file> <key> — value of key=value in a properties file, or empty.
prop() { [ -f "$1" ] && sed -n "s/^[[:space:]]*$2[[:space:]]*=[[:space:]]*//p" "$1" | tail -1 | tr -d '\r' || true; }
