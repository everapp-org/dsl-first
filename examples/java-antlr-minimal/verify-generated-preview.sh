#!/usr/bin/env bash
# Regenerate from domain.dsl and diff against the committed generated-preview/ copies.
# Fails if they drift — i.e. if someone changed the DSL or generator but forgot to
# refresh the readable snapshot. Run locally or in CI.
#
#   ./verify-generated-preview.sh           # verify (exit 1 on drift)
#   ./verify-generated-preview.sh --update  # accept current output as the new snapshot
set -euo pipefail

cd "$(dirname "$0")"

GEN_DIR="target/generated-sources/dsl/org/everapp/generated"
PREVIEW_DIR="generated-preview"
FILES=(AgentState.java Agent.java)

echo "› Regenerating from src/main/resources/domain.dsl …"
mvn -q antlr4:antlr4 compile >/dev/null
mvn -q exec:java -Dexec.mainClass=org.everapp.generator.MainGenerator >/dev/null

if [[ "${1:-}" == "--update" ]]; then
  for f in "${FILES[@]}"; do cp "$GEN_DIR/$f" "$PREVIEW_DIR/$f"; done
  echo "✅ Updated $PREVIEW_DIR/ from freshly generated output."
  exit 0
fi

status=0
for f in "${FILES[@]}"; do
  if diff -u "$PREVIEW_DIR/$f" "$GEN_DIR/$f"; then
    echo "✓ $f matches"
  else
    echo "✗ $f has drifted from generated output"
    status=1
  fi
done

if [[ $status -ne 0 ]]; then
  echo
  echo "generated-preview/ is stale. Run './verify-generated-preview.sh --update' and commit." >&2
fi
exit $status
