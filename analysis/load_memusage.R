# Set directories
if (!exists("basedir")) {
    basedir <- dirname(normalizePath(sys.frame(1)$ofile))
}
datadir <- paste(basedir,'..//output',sep='/')
plotsdir <- paste(basedir,'..//output/plots',sep='/')

# Load data
memusagefile <- paste(datadir,'memusage_master.csv',sep='/')
memusage <- read.table(memusagefile, sep=",", header=TRUE)
memusage$Snapshot <- as.character(memusage$Snapshot)
memusage$Network <- sapply(strsplit(memusage$Snapshot, "/"), "[", 1)
memusage$Timestamp <- sapply(strsplit(memusage$Snapshot, "/"), "[", 2)
memmaster <- memusage

memusagefile <- paste(datadir,'memusage_saw.csv',sep='/')
memusage <- read.table(memusagefile, sep=",", header=TRUE)
memusage$Snapshot <- as.character(memusage$Snapshot)
#memusage$Network <- sapply(strsplit(memusage$Snapshot, "/"), "[", 1)
#memusage$Timestamp <- sapply(strsplit(memusage$Snapshot, "/"), "[", 2)
memsaw <- memusage

# Define plot colors
safecolorsfive <- c('#d7191c','#fdae61','#ffffbf','#abdda4','#2b83ba')
safecolorsfour <- safecolorsfive[c(1,2,4,5)]
safecolorsthree <- c('#fc8d59','#ffffbf','#99d594')
safecolorsthree <- safecolorsfive[c(1,2,5)]
