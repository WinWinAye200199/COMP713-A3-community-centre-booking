#!/bin/bash
# Sends N identical booking requests for the same room and time at the same moment,
# then counts the HTTP status codes. Correct result: exactly one 201, the rest 409.
# Usage: bash scripts/race-test.sh 2026-11-10 20
DATE=${1:-2026-11-10}
N=${2:-20}
seq "$N" | xargs -P "$N" -I{} curl -s -o /dev/null -w "%{http_code}\n" \
  -u member@test.com:member123 -H "Content-Type: application/json" \
  -d "{\"purpose\":\"Race test {}\",\"bookingDate\":\"$DATE\",\"startTime\":\"14:00\",\"endTime\":\"15:00\",\"roomId\":1}" \
  http://localhost:8080/api/bookings | sort | uniq -c
