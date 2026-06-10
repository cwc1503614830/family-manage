const DEFAULT_MAX_BYTES = 1024 * 1024
const MIN_QUALITY = 0.5
const MAX_DIMENSION = 4096
const MIN_DIMENSION = 320

function loadImage(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => {
      URL.revokeObjectURL(url)
      resolve(image)
    }
    image.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('无法读取图片，请换一张再试'))
    }
    image.src = url
  })
}

function canvasToBlob(canvas, type, quality) {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      blob => {
        if (blob) {
          resolve(blob)
          return
        }
        reject(new Error('图片压缩失败'))
      },
      type,
      quality
    )
  })
}

function fitDimensions(width, height, maxDimension) {
  if (width <= maxDimension && height <= maxDimension) {
    return { width, height }
  }
  const ratio = Math.min(maxDimension / width, maxDimension / height)
  return {
    width: Math.max(MIN_DIMENSION, Math.round(width * ratio)),
    height: Math.max(MIN_DIMENSION, Math.round(height * ratio))
  }
}

function buildOutputName(originalName, outputType) {
  const baseName = String(originalName || 'image').replace(/\.[^.]+$/, '') || 'image'
  const extension = outputType === 'image/png' ? '.png' : '.jpg'
  return `${baseName}${extension}`
}

function resolveOutputType(file) {
  if (file.type === 'image/png') {
    return 'image/png'
  }
  return 'image/jpeg'
}

/**
 * 压缩图片，确保结果不超过 maxBytes（默认 1MB）。
 * 已在限制内的图片原样返回。
 */
export async function compressImage(file, maxBytes = DEFAULT_MAX_BYTES) {
  if (!file || !file.type || !file.type.startsWith('image/')) {
    return file
  }
  if (file.size <= maxBytes) {
    return file
  }

  const image = await loadImage(file)
  const outputType = resolveOutputType(file)
  let { width, height } = fitDimensions(image.width, image.height, MAX_DIMENSION)
  let quality = 0.9
  let blob = null

  for (let attempt = 0; attempt < 24; attempt += 1) {
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const context = canvas.getContext('2d')
    context.drawImage(image, 0, 0, width, height)

    blob = await canvasToBlob(
      canvas,
      outputType,
      outputType === 'image/jpeg' ? quality : undefined
    )

    if (blob.size <= maxBytes) {
      return new File([blob], buildOutputName(file.name, outputType), {
        type: outputType,
        lastModified: Date.now()
      })
    }

    if (outputType === 'image/jpeg' && quality > MIN_QUALITY) {
      quality = Math.max(MIN_QUALITY, quality - 0.08)
      continue
    }

    width = Math.max(MIN_DIMENSION, Math.round(width * 0.85))
    height = Math.max(MIN_DIMENSION, Math.round(height * 0.85))
    quality = 0.85

    if (width <= MIN_DIMENSION && height <= MIN_DIMENSION) {
      break
    }
  }

  if (blob && blob.size <= maxBytes) {
    return new File([blob], buildOutputName(file.name, outputType), {
      type: outputType,
      lastModified: Date.now()
    })
  }

  throw new Error('图片压缩后仍超过 1MB，请选择更小的图片')
}
