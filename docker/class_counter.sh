#!/bin/bash
count_class_files() {
    # Initialize total count
    local total_class_count=0

    # Process each specified directory
    for target_dir in "$@"; do
        # Count the number of .class files
        local class_count
        class_count=$(find "$target_dir" -type f -name "*.class" 2>/dev/null | wc -l)

        # Count the number of .class files inside jar files
        local jar_class_count=0
        for jar_file in $(find "$target_dir" -type f -name "*.jar" 2>/dev/null); do
            local count_in_jar
            count_in_jar=$(unzip -l "$jar_file" 2>/dev/null | grep -c '\.class$')
            jar_class_count=$((jar_class_count + count_in_jar))
        done

        # Add the total for this directory
        total_class_count=$((total_class_count + class_count + jar_class_count))
    done

    # Display the final total count
    echo $total_class_count
}

# Pass all arguments to the function
count_class_files $@