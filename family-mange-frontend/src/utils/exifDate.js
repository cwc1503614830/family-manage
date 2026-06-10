const EXIF_DATE_TAGS = ['DateTimeOriginal', 'DateTimeDigitized', 'DateTime']

/**
 * 从图片文件中读取拍摄时间（优先 EXIF，其次文件修改时间）。
 * 返回格式：YYYY-MM-DD HH:mm:ss
 */
export async function readPhotoTakenAt(file) {
  if (!file || !file.type.startsWith('image/')) {
    return formatDateTime(new Date())
  }
  try {
    const sampleSize = Math.min(file.size, 512 * 1024)
    const buffer = await file.slice(0, sampleSize).arrayBuffer()
    const exifDate = parseExifDateFromBuffer(buffer)
    if (exifDate) {
      return exifDate
    }
  } catch (error) {
    // 解析失败时使用兜底时间
  }
  if (file.lastModified) {
    return formatDateTime(new Date(file.lastModified))
  }
  return formatDateTime(new Date())
}

function parseExifDateFromBuffer(buffer) {
  const bytes = new Uint8Array(buffer)
  if (bytes.length < 4 || bytes[0] !== 0xff || bytes[1] !== 0xd8) {
    return null
  }
  const latin1 = bytesToLatin1(bytes)
  for (let i = 0; i < EXIF_DATE_TAGS.length; i += 1) {
    const tag = EXIF_DATE_TAGS[i]
    const index = latin1.indexOf(tag)
    if (index < 0) {
      continue
    }
    const snippet = latin1.slice(index, index + 64)
    const match = snippet.match(/(\d{4}):(\d{2}):(\d{2})[ T](\d{2}):(\d{2}):(\d{2})/)
    if (match) {
      return `${match[1]}-${match[2]}-${match[3]} ${match[4]}:${match[5]}:${match[6]}`
    }
  }
  const generic = latin1.match(/(\d{4}):(\d{2}):(\d{2})[ T](\d{2}):(\d{2}):(\d{2})/)
  if (generic) {
    return `${generic[1]}-${generic[2]}-${generic[3]} ${generic[4]}:${generic[5]}:${generic[6]}`
  }
  return null
}

function bytesToLatin1(bytes) {
  let text = ''
  for (let i = 0; i < bytes.length; i += 1) {
    text += String.fromCharCode(bytes[i])
  }
  return text
}

function formatDateTime(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}
