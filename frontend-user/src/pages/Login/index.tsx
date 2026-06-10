import { useEffect } from 'react'
import { Tabs, Form, Input, Button, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../stores/auth'
import { authApi } from '../../api/auth'

export default function LoginPage() {
  const navigate = useNavigate()
  const { setAuth, isLoggedIn } = useAuthStore()

  useEffect(() => { if (isLoggedIn()) navigate('/') }, [])

  const onLogin = async (values: any) => {
    try {
      const res: any = await authApi.login(values)
      setAuth(res.data.token, res.data.userId, res.data.nickname, res.data.role)
      message.success('欢迎回来 👋')
      navigate('/')
    } catch (e: any) {
      message.error(e.response?.data?.message || '登录失败')
    }
  }

  const onRegister = async (values: any) => {
    try {
      const res: any = await authApi.register({ ...values, role: 2 })
      setAuth(res.data.token, res.data.userId, res.data.nickname, res.data.role)
      message.success('注册成功 🎉')
      navigate('/')
    } catch (e: any) {
      message.error(e.response?.data?.message || '注册失败')
    }
  }

  return (
    <div style={{
      minHeight: 'calc(100vh - 56px)', display: 'flex',
      alignItems: 'center', justifyContent: 'center',
      background: 'radial-gradient(ellipse at 50% 0%, rgba(254,44,85,0.08) 0%, transparent 60%)',
    }}>
      <div style={{
        width: 380, background: 'rgba(28,28,30,0.8)', backdropFilter: 'blur(20px)',
        borderRadius: 20, padding: '36px 32px',
        border: '1px solid rgba(255,255,255,0.06)',
        boxShadow: '0 20px 60px rgba(0,0,0,0.4)',
      }}>
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{
            width: 56, height: 56, borderRadius: 16, margin: '0 auto 16px',
            background: 'linear-gradient(135deg, #fe2c55 0%, #ff6b81 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28,
          }}>📺</div>
          <div style={{ fontSize: 20, fontWeight: 700, color: '#fff', marginBottom: 4 }}>直播竞拍</div>
          <div style={{ fontSize: 13, color: 'rgba(255,255,255,0.4)' }}>发现好物，实时竞拍</div>
        </div>

        <Tabs
          centered
          items={[
            {
              key: 'login', label: '登录',
              children: (
                <Form onFinish={onLogin} layout="vertical">
                  <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                    <Input placeholder="用户名" size="large" style={{ borderRadius: 10, height: 44 }} />
                  </Form.Item>
                  <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                    <Input.Password placeholder="密码" size="large" style={{ borderRadius: 10, height: 44 }} />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" size="large" block
                    style={{ borderRadius: 12, height: 46, fontWeight: 600, fontSize: 15, background: 'linear-gradient(135deg, #fe2c55 0%, #ff6b81 100%)', border: 'none' }}>
                    登录
                  </Button>
                </Form>
              ),
            },
            {
              key: 'register', label: '注册',
              children: (
                <Form onFinish={onRegister} layout="vertical">
                  <Form.Item name="username" rules={[{ required: true }]}>
                    <Input placeholder="用户名" size="large" style={{ borderRadius: 10, height: 44 }} />
                  </Form.Item>
                  <Form.Item name="password" rules={[{ required: true }]}>
                    <Input.Password placeholder="密码" size="large" style={{ borderRadius: 10, height: 44 }} />
                  </Form.Item>
                  <Form.Item name="nickname" rules={[{ required: true }]}>
                    <Input placeholder="昵称" size="large" style={{ borderRadius: 10, height: 44 }} />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" size="large" block
                    style={{ borderRadius: 12, height: 46, fontWeight: 600, fontSize: 15, background: 'linear-gradient(135deg, #fe2c55 0%, #ff6b81 100%)', border: 'none' }}>
                    注册
                  </Button>
                </Form>
              ),
            },
          ]}
        />
      </div>
    </div>
  )
}
