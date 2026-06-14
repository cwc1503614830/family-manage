import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue2'

export default defineConfig({
  plugins: [
    vue(),
    {
      name: 'root-index-fallback',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          if (req.url === '/') {
            req.url = '/index.html'
          }
          next()
        })
      }
    }
  ],
  server: {
    host: '0.0.0.0',
    // 花生壳等内网穿透：允许 vicp 域名访问开发服务
    allowedHosts: ['.vicp.fun', '.vicp.cc', '.vicp.net', 'localhost','zicp.vip','3961l8y412.zicp.vip'],
    proxy: {
      '/api': {
        target: 'http://localhost:12300',
        changeOrigin: true
      }
    }
  }
})
