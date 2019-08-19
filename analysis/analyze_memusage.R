source('load_memusage.R')

memboth <- merge(memmaster, memsaw, by='Snapshot', suffixes=c('Master','Saw'))
memboth <- na.omit(memboth)
memboth <- memboth[which(memboth$EtgsMaster > 0),]

print("Number of snapshots:")
print(nrow(memboth))

feweretgs <- memboth[which(memboth$EtgsSaw < memboth$EtgsMaster),]
print("Number of snapshots requiring fewer ETGs:")
print(nrow(feweretgs))

lessmem <- feweretgs[which(feweretgs$MemSaw < feweretgs$MemMaster),]
print("Number of snapshots requiring less memory:")
print(nrow(lessmem))

lessmem <- memboth

lessmem$MemChange <- (lessmem$MemSaw - lessmem$MemMaster) / lessmem$MemMaster
lessmem$EtgChange <- (lessmem$EtgsSaw - lessmem$EtgsMaster) / lessmem$EtgsMaster
lessmem$MemChange <- round(lessmem$MemChange * 100)
lessmem$EtgChange <- round(lessmem$EtgChange * 100)
lessmem <- lessmem[order(lessmem$MemChange,lessmem$EtgChange),]

print("Percent of snapshots requiring at least 50% less memory:")
print(length(which(lessmem$MemChange <= -50)) / nrow(lessmem) * 100)

print("Percent of snapshots requiring at least 50% fewer ETGs:")
print(length(which(lessmem$EtgChange <= -50)) / nrow(lessmem) * 100)

plotfile <- paste(plotsdir,'analyze_memusage.pdf',sep='/')
pdf(plotfile, height=3, width=6)
#png(plotfile, height=300, width=300)
par(mar=c(2.8,2.8,0.5,0.5),mgp=c(1.6,0.4,0))

x <- seq(1, nrow(lessmem))
y <- lessmem$MemChange
plot(x, y, xlim=c(0,length(x)), ylim=c(-100,100), 
    xlab='Network', ylab='% Change', pch=1, type='o',
    col=safecolorsfive[1], cex.axis=1.2, cex.lab=1.2)

y <- lessmem$EtgChange
points(x, y, col=safecolorsfive[2], pch=2, type='o')

lines(c(0,length(x)), c(0,0), col='black')

legend(0,100,c('Memory','ETGs'),
    pch=c(1,2), lwd=1, col=c(safecolorsfive[1],safecolorsfive[2]), bty='n')

dev.off()

tmp <- lessmem[which(feweretgs$MemSaw < feweretgs$MemMaster),]
tmp$MemDiff <- (tmp$MemMaster - tmp$MemSaw) / 1000
tmp <- tmp[order(tmp$MemDiff),]

print("Percent of memory-saving snapshots that use at least 1MB less:")
print(length(which(tmp$MemDiff >= 1000)) / nrow(tmp) * 100)

plotfile <- paste(plotsdir,'memusage_absolute.pdf',sep='/')
pdf(plotfile, height=3, width=6)
#png(plotfile, height=300, width=300)
par(mar=c(2.8,2.8,0.5,0.5),mgp=c(1.6,0.4,0))

x <- seq(1, nrow(tmp))
y <- log((tmp$MemDiff))/log(10)
plot(x, y, xlim=c(0,length(x)), ylim=c(0,6), 
    xlab='Network', ylab='Memory reduction (KB)', pch=1, type='o',
    col=safecolorsfive[1], cex.axis=1.2, cex.lab=1.2, yaxt='n')

axis(2, at=seq(0,6), labels=c('0','10',expression(paste('10'^'2')),
    expression(paste('10'^'3')),expression(paste('10'^'4')),
    expression(paste('10'^'5')),expression(paste('10'^'6'))))

dev.off()

