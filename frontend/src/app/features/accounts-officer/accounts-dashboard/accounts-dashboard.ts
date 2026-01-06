import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth';
import { forkJoin } from 'rxjs';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-accounts-dashboard',
  standalone: true,
  imports: [CommonModule, SidebarComponent, NavbarComponent, BaseChartDirective, RouterModule],
  templateUrl: './accounts-dashboard.html',
  styles: [`
    .stat-card { transition: transform 0.2s; }
    .stat-card:hover { transform: translateY(-3px); }
  `]
})
export class AccountsDashboardComponent implements OnInit {
  
  totalRevenue = 0;
  todayRevenue = 0;
  outstandingDues = 0;
  recentTransactions: any[] = [];
  
  public pieChartData: ChartConfiguration<'pie'>['data'] = {
    labels: ['UPI', 'Card', 'NetBanking'],
    datasets: [{ data: [0, 0, 0] }]
  };
  public pieChartOptions: ChartConfiguration<'pie'>['options'] = { responsive: true };

  public lineChartData: ChartConfiguration<'line'>['data'] = {
    labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'],
    datasets: [{
      data: [0, 0, 0, 0, 0],
      label: 'Daily Collection (₹)',
      fill: true,
      tension: 0.4,
      borderColor: '#0d6efd',
      backgroundColor: 'rgba(13, 110, 253, 0.1)'
    }]
  };
  public lineChartOptions: ChartConfiguration<'line'>['options'] = { responsive: true };

  constructor(
    private http: HttpClient, 
    private authService: AuthService,
    private cd: ChangeDetectorRef 
  ) {}

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    const token = this.authService.getToken();
    const headers = { Authorization: `Bearer ${token}` };

    forkJoin({
      stats: this.http.get<any>('http://localhost:8080/payments/stats', { headers }),
      history: this.http.get<any[]>('http://localhost:8080/payments/history', { headers }),
      pending: this.http.get<any[]>('http://localhost:8080/bills/pending', { headers })
    }).subscribe({
      next: ({ stats, history, pending }) => {
        this.totalRevenue = stats.totalRevenue || 0;
        this.todayRevenue = stats.todayRevenue || 0;
        this.outstandingDues = pending.reduce((acc: number, bill: any) => acc + bill.totalAmount, 0);        
        this.recentTransactions = history.slice(0, 10);        
        const upiCount = history.filter(p => p.paymentMode === 'UPI').length;
        const cardCount = history.filter(p => p.paymentMode === 'CARD').length;
        const netCount = history.filter(p => p.paymentMode === 'NET_BANKING').length;
        this.pieChartData = {
            labels: ['UPI', 'Card', 'NetBanking'],
            datasets: [{ data: [upiCount, cardCount, netCount], backgroundColor: ['#198754', '#0d6efd', '#ffc107'] }]
        };
        this.cd.detectChanges();
      },
      error: (err) => console.error(err)
    });
  }
}