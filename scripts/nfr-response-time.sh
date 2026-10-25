#!/bin/bash
# NFR response-time test for POST /api/bookings (create booking).
# Sends N requests using C concurrent clients. Every request books a different date,
# so all requests should succeed (201). All requests use the same room to include
# waiting on the room row lock (worst case for the double-booking fix).
# Usage:   bash scripts/nfr-response-time.sh [requests] [concurrency] [roomId] [year]
# Example: bash scripts/nfr-response-time.sh 200 10 3 2027   (max 336 requests per year)

if [ "$1" = "--one" ]; then
  i=$2; room=$3; year=$4
  month=$(( (i - 1) / 28 + 1 ))
  day=$(( (i - 1) % 28 + 1 ))
  date=$(printf "%d-%02d-%02d" "$year" "$month" "$day")
  curl -s -o /dev/null -w "%{http_code} %{time_total}\n" \
    -u member@test.com:member123 -H "Content-Type: application/json" \
    -d "{\"purpose\":\"NFR test $i\",\"bookingDate\":\"$date\",\"startTime\":\"10:00\",\"endTime\":\"11:00\",\"roomId\":$room}" \
    http://localhost:8080/api/bookings
  exit 0
fi

N=${1:-200}; C=${2:-10}; ROOM=${3:-3}; YEAR=${4:-2027}
mkdir -p docs/nfr
OUT="docs/nfr/results-n${N}-c${C}-$(date +%Y%m%d-%H%M%S).txt"

echo "POST /api/bookings | requests=$N concurrency=$C room=$ROOM year=$YEAR" | tee "$OUT.summary"
seq "$N" | xargs -P "$C" -I{} bash "$0" --one {} "$ROOM" "$YEAR" > "$OUT"

echo "Status codes:" | tee -a "$OUT.summary"
cut -d' ' -f1 "$OUT" | sort | uniq -c | tee -a "$OUT.summary"

sort -k2 -n "$OUT" | awk '{ t[NR] = $2 * 1000; sum += $2 * 1000 }
  END {
    n = NR
    p50 = t[int(n * 0.50 + 0.999)]
    p95 = t[int(n * 0.95 + 0.999)]
    printf "Requests: %d\nAverage: %.0f ms\nMedian (p50): %.0f ms\n95th percentile: %.0f ms\nMax: %.0f ms\n", n, sum / n, p50, p95, t[n]
  }' | tee -a "$OUT.summary"

echo "Raw results: $OUT"
