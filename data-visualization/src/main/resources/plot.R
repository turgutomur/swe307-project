# Load required library
library(lattice)

# Create data frame from passed variables
randomData <- data.frame(
    time = time,
    RandomData = data
)

# Create temporary SVG file
svg_file <- tempfile(fileext = ".svg")
svg(svg_file, width = 10, height = 6)

# Generate xyplot with specifications from project document
# - Line plot with dark brown color
# - Grid enabled
# - x-axis: 0-99 (time)
# - y-axis: double values
plot <- xyplot(RandomData ~ time,
    data = randomData,
    type = c('l', 'g'),
    col.line = 'saddlebrown',
    lwd = 2,
    main = 'Random Number Plot',
    xlab = 'Time',
    ylab = 'Random Data',
    grid = TRUE,
    scales = list(
        x = list(at = seq(0, 100, by = 10)),
        y = list(relation = "free")
    )
)

# Print plot to SVG
print(plot)
dev.off()

# Read SVG file content
svg_content <- paste(readLines(svg_file), collapse = "\n")

# Clean up temporary file
unlink(svg_file)

# Return SVG content
svg_content