import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { jsPDF } from 'jspdf';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';
import { AuthService } from '../../../core/services/auth';
import { ConsumerService } from '../../../core/services/consumer';
import { BillingService } from '../../../core/services/billing';

@Component({
  selector: 'app-transaction-ledger',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent, NavbarComponent],
  providers: [DatePipe],
  templateUrl: './transaction-ledger.html',
  styles: [`
    .table th { font-weight: 600; font-size: 0.85rem; text-transform: uppercase; letter-spacing: 0.5px; }
    .badge-mode { font-size: 0.75rem; padding: 6px 12px; font-weight: 600; letter-spacing: 0.5px; }
  `]
})
export class TransactionLedgerComponent implements OnInit {
  
  allPayments: any[] = [];
  filteredPayments: any[] = [];
  searchQuery = '';
  selectedMode = 'ALL';
  isLoading = true;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private consumerService: ConsumerService,
    private billingService: BillingService,
    private datePipe: DatePipe,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadLedgerData();
  }

  loadLedgerData() {
    this.isLoading = true;
    const token = this.authService.getToken();
    const headers = { Authorization: `Bearer ${token}` };

    forkJoin({
      payments: this.http.get<any[]>('http://localhost:8080/payments/history', { headers }),
      bills: this.billingService.getAllBills(),
      connections: this.consumerService.getAllConnections()
    }).subscribe({
      next: ({ payments, bills, connections }) => {
        
        this.allPayments = payments.map(payment => {
          const relatedBill = bills.find((b: any) => b.id === payment.billId);
          const relatedConn = relatedBill ? connections.find((c: any) => c.id === relatedBill.connectionId) : null;
          
          return {
            ...payment,
            consumerName: relatedConn ? relatedConn.consumerName : 'Unknown Consumer',
            utilityType: relatedConn ? relatedConn.utilityType : 'N/A'
          };
        });

        this.allPayments.sort((a, b) => new Date(b.paymentDate).getTime() - new Date(a.paymentDate).getTime());
        this.applyFilters();
        
        this.isLoading = false;
        this.cd.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load ledger', err);
        this.isLoading = false; 
        this.cd.detectChanges();
      }
    });
  }

  applyFilters() {
    let temp = [...this.allPayments];
    if (this.selectedMode !== 'ALL') {
      if (this.selectedMode === 'CARD') {
        temp = temp.filter(p => p.paymentMode.includes('CARD'));
      } else {
        temp = temp.filter(p => p.paymentMode === this.selectedMode);
      }
    }

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      temp = temp.filter(p => 
        p.transactionId.toLowerCase().includes(q) ||
        p.consumerName.toLowerCase().includes(q)
      );
    }

    this.filteredPayments = temp;
    this.cd.detectChanges();
  }

  exportCSV() {
    if (this.filteredPayments.length === 0) return;

    const headers = ['Transaction ID', 'Date', 'Consumer', 'Utility', 'Mode', 'Amount'];
    const rows = this.filteredPayments.map(p => [
      p.transactionId,
      this.datePipe.transform(p.paymentDate, 'short'),
      p.consumerName,
      p.utilityType,
      p.paymentMode,
      p.amount
    ]);

    let csvContent = "data:text/csv;charset=utf-8,";
    csvContent += headers.join(",") + "\r\n";
    rows.forEach(row => {
      csvContent += row.join(",") + "\r\n";
    });

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `Utilix_Ledger_${new Date().toISOString().slice(0,10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  downloadReceipt(payment: any) {
    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();
    const centerX = pageWidth / 2;
    doc.setFillColor(37, 99, 235);
    doc.rect(0, 0, pageWidth, 40, 'F');
    doc.setFontSize(22);
    doc.setTextColor(255, 255, 255);
    doc.text('PAYMENT RECEIPT', centerX, 20, { align: 'center' });
    doc.setFontSize(10);
    doc.text('Utilix Services Pvt Ltd', centerX, 30, { align: 'center' });
    doc.setTextColor(0, 0, 0);
    doc.setFontSize(10);    
    doc.setDrawColor(200);
    doc.setFillColor(250, 250, 250);
    doc.rect(120, 50, 70, 25, 'FD');
    doc.setFontSize(9);
    doc.setTextColor(100);
    doc.text('Receipt Date:', 125, 60);
    doc.text('Receipt No:', 125, 68);
    doc.setFontSize(10);
    doc.setTextColor(0);
    doc.text(this.datePipe.transform(new Date(), 'mediumDate') || '', 185, 60, { align: 'right' });
    doc.text(`#${payment.transactionId.substring(4, 12)}`, 185, 68, { align: 'right' });
    let y = 90;
    const leftCol = 20;
    const rightCol = 100;
    const rowHeight = 12;
    const drawRow = (label: string, value: string) => {
      doc.setFontSize(11);
      doc.setTextColor(100);
      doc.text(label, leftCol, y);
      doc.setFontSize(12);
      doc.setTextColor(0);
      doc.text(value, rightCol, y);      
      doc.setDrawColor(240);
      doc.line(leftCol, y + 4, 190, y + 4);
      y += rowHeight;
    };
    drawRow('Transaction ID', payment.transactionId);
    drawRow('Payment Date', this.datePipe.transform(payment.paymentDate, 'medium') || '');
    drawRow('Payment Mode', payment.paymentMode);
    drawRow('Consumer Name', payment.consumerName || 'N/A');
    drawRow('Utility Service', payment.utilityType || 'N/A');
    y += 10;
    doc.setFillColor(240, 255, 244);
    doc.setDrawColor(200, 230, 200);
    doc.rect(leftCol, y, 170, 25, 'FD');
    doc.setFontSize(14);
    doc.setTextColor(0, 100, 0);
    doc.text('TOTAL AMOUNT PAID', leftCol + 10, y + 17);
    doc.setFontSize(20);
    doc.text(`Rs. ${payment.amount}`, 180, y + 17, { align: 'right' });
    doc.setFontSize(9);
    doc.setTextColor(150);
    doc.text('This is a computer-generated receipt and does not require a physical signature.', centerX, 280, { align: 'center' });
    doc.text('For support, contact help@utilix.com', centerX, 285, { align: 'center' });
    doc.save(`Receipt_${payment.transactionId}.pdf`);
  }
}