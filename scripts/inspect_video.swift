import Foundation
import AVFoundation
import AppKit
let path = CommandLine.arguments[1]
let asset = AVURLAsset(url: URL(fileURLWithPath: path))
let generator = AVAssetImageGenerator(asset: asset)
generator.appliesPreferredTrackTransform = true
generator.requestedTimeToleranceBefore = .zero
generator.requestedTimeToleranceAfter = .zero
generator.maximumSize = CGSize(width: 640, height: 640)
for (i, seconds) in [1.0,1.1,1.2,1.3,1.4,1.5,1.6,1.7,1.8].enumerated() {
    do {
        let cg = try generator.copyCGImage(at: CMTime(seconds: seconds, preferredTimescale: 600), actualTime: nil)
        let rep = NSBitmapImageRep(cgImage: cg)
        try rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: "artifacts/effect-frame-\(i).png"))
    } catch { print(error) }
}
