import React from 'react';
import ReactDOM from 'react-dom/client';
import {BrowserRouter} from 'react-router-dom';
import App from './App';
import './styles/global.css';

class ErrorBoundary extends React.Component<{children: React.ReactNode}, {error: Error | null}> {
  constructor(props: {children: React.ReactNode}) {
    super(props);
    this.state = {error: null};
  }
  static getDerivedStateFromError(error: Error) {
    return {error};
  }
  render() {
    if (this.state.error) {
      return (
        <div style={{padding: 24, fontFamily: 'sans-serif'}}>
          <h2 style={{color: '#EF4444'}}>앱 오류가 발생했습니다</h2>
          <pre style={{marginTop: 12, fontSize: 12, color: '#6B7280', whiteSpace: 'pre-wrap'}}>
            {this.state.error.message}
          </pre>
        </div>
      );
    }
    return this.props.children;
  }
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ErrorBoundary>
  </React.StrictMode>,
);
