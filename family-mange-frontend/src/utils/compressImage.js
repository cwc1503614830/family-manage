/**
 * Compress image before upload (for slow networks / intranet tunnel).
 */
function canvasToBlob(canvas, quality) {
  return new Promise((resolve) => {
    canvas.toBlob((blob) => resolve(blob), 'image/jpeg', quality)
  })
}

async function encodeCanvas(canvas, targetSizeKB, quality) {
  let blob = await canvasToBlob(canvas, quality)
  if (!blob) {
    return null
  }

  while (blob.size > targetSizeKB * 1024 && quality > 0.45) {
    quality = Math.max(0.45, quality - 0.12)
    blob = await canvasToBlob(canvas, quality)
    if (!blob) {
      return null
    }
  }

  return blob
}

export async function compressImage(file, options = {}) {
  const {
    maxWidth = 1280,
    maxHeight = 1280,
    quality = 0.78,
    maxSizeKB = 200,
    targetSizeKB = 800
  } = options

  if (!file || !file.type.startsWith('image/') || file.type === 'image/gif') {
    return file
  }

  if (file.size <= maxSizeKB * 1024) {
    return file
  }

  const objectUrl = URL.createObjectURL(file)

  try {
    const img = await new Promise((resolve, reject) => {
      const image = new Image()
      image.onload = () => resolve(image)
      image.onerror = () => reject(new Error('image load failed'))
      image.src = objectUrl
    })

    let width = img.width
    let height = img.height
    const scale = Math.min(maxWidth / width, maxHeight / height, 1)
    width = Math.max(1, Math.round(width * scale))
    height = Math.max(1, Math.round(height * scale))

    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      return file
    }

    ctx.drawImage(img, 0, 0, width, height)

    const blob = await encodeCanvas(canvas, targetSizeKB, quality)
    if (!blob) {
      return file
    }

    const baseName = file.name.replace(/\.[^.]+$/, '') || 'image'
    const compressed = new File([blob], `${baseName}.jpg`, {
      type: 'image/jpeg',
      lastModified: Date.now()
    })

    console.info(
      `[upload] compress ${file.name}: ${(file.size / 1024).toFixed(0)}KB -> ${(compressed.size / 1024).toFixed(0)}KB`
    )

    return compressed.size < file.size ? compressed : file
  } catch (error) {
    console.warn('[upload] compress skipped:', file.name, error)
    return file
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}
