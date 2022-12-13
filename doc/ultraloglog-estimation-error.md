### UltraLogLog estimation error

The state of an UltraLogLog sketch with precision parameter $p$ requires $m = 2^p$ bytes.
The expected relative standard error is approximately given by $\frac{0.785}{\sqrt{m}}$ and $\frac{0.658}{\sqrt{m}}$ for the default and the martingale estimator, respectively.
This is a good approximation for all $p\geq 6$ and large distinct counts.
However, the error is significantly smaller for distinct counts that are in the order of $m$ or smaller.
The bias is always much smaller than the root-mean-square error (rmse) and can therefore be neglected.
The following charts show the empirically evaluated relative error as a function of the true distinct count for various precision parameters $p$ based on 100k simulation runs:

<img src="../test-results/ultraloglog-estimation-error-p3.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p4.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p5.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p6.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p7.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p8.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p9.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p10.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p11.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p12.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p13.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p14.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p15.png" width="400"><img src="../test-results/ultraloglog-estimation-error-p16.png" width="400">
