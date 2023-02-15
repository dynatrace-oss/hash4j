package main

import (
	"crypto/sha256"
	"fmt"
	"github.com/kalafut/imohash"
)

func main() {
	maxLength := 200000
    b := make([]byte, maxLength)
	checksumCalculator := sha256.New()
	for i := 0; i < maxLength; i++ {
		b[i] = byte(i * i + 31);
	}	
	for l := 0; l < maxLength; l++ {
		hash := imohash.Sum(b[0:l])
	   	checksumCalculator.Write(hash[:])
	}
	
	checkSum := checksumCalculator.Sum(nil)
	fmt.Printf("%032x\n", checkSum)
}