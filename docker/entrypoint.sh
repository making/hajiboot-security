#!/bin/bash
get_container_memory_limit() {
	# Function to check if a file exists and contains a numeric value
	check_numeric_file() {
		[ -f "$1" ] && grep -q '^[0-9]\+$' "$1"
	}

	# Check if memory.limit_in_bytes exists and is numeric
	if check_numeric_file "/sys/fs/cgroup/memory/memory.limit_in_bytes"; then
		cat /sys/fs/cgroup/memory/memory.limit_in_bytes
	elif check_numeric_file "/sys/fs/cgroup/memory.max"; then
		cat /sys/fs/cgroup/memory.max
	elif grep -q '^MemTotal:' /proc/meminfo && awk '/^MemTotal:/ {print $2}' /proc/meminfo | grep -q '^[0-9]\+$'; then
		# Get MemTotal from /proc/meminfo and convert to bytes (kB to bytes)
		awk '/^MemTotal:/ {print $2 * 1024}' /proc/meminfo
	else
		echo "Unable to determine memory limit or file contents are invalid." >&2
		return 1
	fi
}

TOTAL_MEMORY=$(get_container_memory_limit)
if [ "$TOTAL_MEMORY" = "9223372036854771712" ];then
  TOTAL_MEMORY=1G
fi
LOADED_CLASS_COUNT=$(cat /opt/class_count)
# Load Factor = 60%
LOADED_CLASS_COUNT=$((LOADED_CLASS_COUNT * 60 / 100))
JVM_MEMORY_CONFIGURATION=$(java-buildpack-memory-calculator-linux \
  -totMemory "${TOTAL_MEMORY}B" \
  -loadedClasses "${BPL_JVM_LOADED_CLASS_COUNT:-$LOADED_CLASS_COUNT}" \
  -stackThreads "${BPL_JVM_THREAD_COUNT:-100}" \
  -vmOptions "${JAVA_TOOL_OPTIONS}" \
  -headRoom "${BPL_JVM_HEAD_ROOM:-0}" \
  -poolType metaspace)

echo "JVM Memory Configuration: ${JVM_MEMORY_CONFIGURATION}"
export JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError ${JVM_MEMORY_CONFIGURATION} ${JAVA_TOOL_OPTIONS}"

java org.springframework.boot.loader.JarLauncher &
JAVA_PID=$!

stop_java_app() {
    kill -SIGTERM $JAVA_PID
    sleep 1
}

trap stop_java_app SIGINT
wait $JAVA_PID