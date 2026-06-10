import { useEffect } from 'react'
import { Tabs, Form, Input, Button, Select, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../stores/auth'
import { authApi } from '../../api/auth'

export default function LoginPage() {
  const navigate = useNavigate()
  const { setAuth, isLoggedIn } = useAuthStore()

  useEffect(() => {
    if (isLoggedIn()) navigate('/admin/auctions')
  }, [])

  const onLogin = async (values: any) => {
    try {
      const res: any = await authApi.login(values)
      if (res.data.role !== 1) {
        message.error('此账号为普通用户，请使用移动端参与竞拍')
        return
      }
      setAuth(res.data.token, res.data.userId, res.data.nickname, res.data.role)
      message.success('登录成功')
      navigate('/admin/auctions')
    } catch (e: any) {
      message.error(e.response?.data?.message || '登录失败')
    }
  }

  const onRegister = async (values: any) => {
    try {
      const res: any = await authApi.register({ ...values, role: 1 })
      if (res.data.role !== 1) {
        message.error('此账号为普通用户，请使用移动端参与竞拍')
        return
      }
      setAuth(res.data.token, res.data.userId, res.data.nickname, res.data.role)
      message.success('注册成功')
      navigate('/admin/auctions')
    } catch (e: any) {
      message.error(e.response?.data?.message || '注册失败')
    }
  }

  return (
    <div style={{ maxWidth: 400, margin: '100px auto' }}>
      <h2 style={{ textAlign: 'center' }}>直播竞拍管理后台</h2>
      <Tabs
        items={[
          {
            key: 'login',
            label: '登录',
            children: (
              <Form onFinish={onLogin}>
                <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                  <Input placeholder="用户名" />
                </Form.Item>
                <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                  <Input.Password placeholder="密码" />
                </Form.Item>
                <Button type="primary" htmlType="submit" block>登录</Button>
              </Form>
            ),
          },
          {
            key: 'register',
            label: '注册',
            children: (
              <Form onFinish={onRegister}>
                <Form.Item name="username" rules={[{ required: true }]}>
                  <Input placeholder="用户名" />
                </Form.Item>
                <Form.Item name="password" rules={[{ required: true }]}>
                  <Input.Password placeholder="密码" />
                </Form.Item>
                <Form.Item name="nickname" rules={[{ required: true }]}>
                  <Input placeholder="昵称" />
                </Form.Item>
                <Button type="primary" htmlType="submit" block>注册主播账号</Button>
              </Form>
            ),
          },
        ]}
      />
    </div>
  )
}
